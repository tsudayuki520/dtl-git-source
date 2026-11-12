package com.dlust.sportbackend.concurrency;

import com.dlust.sportbackend.Mapper.*;
import com.dlust.sportbackend.Service.Impl.*;
import com.dlust.sportbackend.entity.Participant;
import io.lettuce.core.ClientOptions;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in: creates/drops ONLY the named disposable database. Never runs with mvn test alone. */
@EnabledIfSystemProperty(named = "sport.it", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrationConcurrencyIntegrationTest {
    private static final String DATABASE = "sport_concurrency_test_20261112";
    private final String redisPrefix = "sport:test:20261112:" + UUID.randomUUID() + ":";
    private JdbcTemplate admin;
    private JdbcTemplate jdbc;
    private boolean created;
    private DriverManagerDataSource dataSource;
    private SqlSessionTemplate session;
    private LettuceConnectionFactory redisFactory;
    private StringRedisTemplate redis;
    private RegistrationServiceImpl first;
    private RegistrationServiceImpl second;

    @BeforeAll
    @SuppressWarnings("unchecked")
    void createIsolatedEnvironment() throws Exception {
        Map<String, Object> config;
        try (var input = Files.newInputStream(Path.of("src/main/resources/application.yml"))) {
            config = new Yaml().load(input);
        }
        Map<String, Object> source = (Map<String, Object>) ((Map<String, Object>) config.get("spring")).get("datasource");
        String url = source.get("url").toString();
        // Only accept an ordinary JDBC MySQL URL; do not silently use the production schema.
        assertTrue(url.matches("jdbc:mysql://[^/]+/[^?]+(?:\\?.*)?"));
        String adminUrl = url.replaceFirst("(jdbc:mysql://[^/]+/)[^?]+", "$1mysql");
        String testUrl = url.replaceFirst("(jdbc:mysql://[^/]+/)[^?]+", "$1" + DATABASE);
        String username = source.get("username").toString();
        String password = source.get("password").toString();
        admin = new JdbcTemplate(new DriverManagerDataSource(adminUrl, username, password));
        // No IF NOT EXISTS: if a database of this name exists, abort without touching it.
        admin.execute("CREATE DATABASE " + DATABASE + " CHARACTER SET utf8mb4");
        created = true;
        dataSource = new DriverManagerDataSource(testUrl, username, password);
        jdbc = new JdbcTemplate(dataSource);
        try (var connection = dataSource.getConnection()) {
            for (String table : List.of("sports_meeting", "user", "group_type", "team", "participant",
                    "event", "schedule", "event_schedule", "registration", "result")) {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource("sql/" + table + ".sql"));
            }
        }
        var factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeAliasesPackage("com.dlust.sportbackend.entity");
        factory.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        var mybatis = new org.apache.ibatis.session.Configuration();
        mybatis.setMapUnderscoreToCamelCase(true);
        mybatis.addMapper(RegistrationLockMapper.class);
        factory.setConfiguration(mybatis);
        session = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        var server = new RedisStandaloneConfiguration("127.0.0.1", 6379);
        var client = LettuceClientConfiguration.builder().commandTimeout(Duration.ofSeconds(2))
                .clientOptions(ClientOptions.builder().autoReconnect(true).build()).build();
        redisFactory = new LettuceConnectionFactory(server, client);
        redisFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(redisFactory);
        first = service(gate());
        second = service(gate());
    }

    @AfterAll
    void cleanup() {
        try {
            if (redis != null) {
                Set<String> keys = redis.keys(redisPrefix + "*");
                if (keys != null && !keys.isEmpty()) redis.delete(keys);
            }
        } finally {
            if (redisFactory != null) redisFactory.destroy();
            if (created) admin.execute("DROP DATABASE " + DATABASE);
        }
    }

    private RedisRegistrationGate gate() {
        return new RedisRegistrationGate(redis, redisPrefix, 30_000, 10_000);
    }

    private RegistrationServiceImpl service(RegistrationGate admission) {
        var schedules = new EventScheduleServiceImpl();
        ReflectionTestUtils.setField(schedules, "eventScheduleMapper", session.getMapper(EventScheduleMapper.class));
        ReflectionTestUtils.setField(schedules, "eventMapper", session.getMapper(EventMapper.class));
        var results = new ResultServiceImpl();
        ReflectionTestUtils.setField(results, "resultMapper", session.getMapper(ResultMapper.class));
        ReflectionTestUtils.setField(results, "eventScheduleMapper", session.getMapper(EventScheduleMapper.class));
        var service = new RegistrationServiceImpl();
        ReflectionTestUtils.setField(service, "registrationMapper", session.getMapper(RegistrationMapper.class));
        ReflectionTestUtils.setField(service, "participantMapper", session.getMapper(ParticipantMapper.class));
        ReflectionTestUtils.setField(service, "eventMapper", session.getMapper(EventMapper.class));
        ReflectionTestUtils.setField(service, "teamMapper", session.getMapper(TeamMapper.class));
        ReflectionTestUtils.setField(service, "groupTypeMapper", session.getMapper(GroupTypeMapper.class));
        ReflectionTestUtils.setField(service, "sportsMeetingMapper", session.getMapper(SportsMeetingMapper.class));
        ReflectionTestUtils.setField(service, "eventScheduleService", schedules);
        ReflectionTestUtils.setField(service, "resultService", results);
        ReflectionTestUtils.setField(service, "writeExecutor", new RegistrationWriteExecutor(admission,
                session.getMapper(RegistrationLockMapper.class), new DataSourceTransactionManager(dataSource)));
        return service;
    }

    @BeforeEach
    void seed() {
        for (String table : List.of("result", "registration", "participant", "event_schedule", "schedule",
                "event", "team", "group_type", "user", "sports_meeting")) jdbc.execute("DELETE FROM " + table);
        jdbc.update("INSERT INTO sports_meeting (id,name,status,organizer,contact_phone,venue,registration_start,registration_end,competition_date) "
                + "VALUES (1,'test',1,'test','0','test',NOW()-INTERVAL 1 DAY,NOW()+INTERVAL 1 DAY,CURDATE())");
        jdbc.update("INSERT INTO group_type (id,sports_meeting_id,name) VALUES (1,1,'test')");
        jdbc.update("INSERT INTO team (id,sports_meeting_id,group_type_id,name) VALUES (1,1,1,'test')");
        for (int i = 1; i <= 32; i++) {
            jdbc.update("INSERT INTO user (id,user_code,password,name,gender) VALUES (?,?,'test','test','男')", i, "test" + i);
            jdbc.update("INSERT INTO participant (id,sports_meeting_id,user_id,team_id) VALUES (?,1,?,1)", i, i);
        }
        for (int id = 1; id <= 2; id++) {
            jdbc.update("INSERT INTO event (id,sports_meeting_id,name,group_type_id) VALUES (?,1,'test',1)", id);
            jdbc.update("INSERT INTO schedule (id,sports_meeting_id,name,sort) VALUES (?,1,?,?)", id, "round" + id, id);
        }
        jdbc.update("INSERT INTO event_schedule (id,event_id,schedule_id,allow_register) VALUES (1,1,1,1),(2,1,2,0),(3,2,1,1)");
    }

    private int race(int requests, IntConsumer action) throws Exception {
        var pool = Executors.newFixedThreadPool(Math.min(16, requests));
        var start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                final int index = i;
                futures.add(pool.submit(() -> {
                    start.await();
                    try { action.accept(index); return true; }
                    catch (RuntimeException e) {
                        assertTrue(e.getMessage().startsWith("超出限报") || e.getMessage().equals("您已报名该项目"),
                                () -> "Unexpected failure: " + e);
                        return false;
                    }
                }));
            }
            start.countDown();
            int accepted = 0;
            for (var future : futures) if (future.get(30, TimeUnit.SECONDS)) accepted++;
            return accepted;
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(20, TimeUnit.SECONDS));
        }
    }

    private int count(String sql) { return jdbc.queryForObject(sql, Integer.class); }
    private Long registrationId(long participant) {
        return jdbc.queryForObject("SELECT id FROM registration WHERE participant_id=? AND schedule_id=1", Long.class, participant);
    }

    @Test
    void redisAndMysqlPreventEventOverbookingAcrossTwoServiceInstances() throws Exception {
        jdbc.update("UPDATE event SET register_limit=5 WHERE id=1");
        assertEquals(5, race(32, i -> (i % 2 == 0 ? first : second).add(i + 1L, 1L, 1L)));
        assertEquals(5, count("SELECT COUNT(*) FROM registration WHERE event_id=1 AND status=0"));
        assertTrue(redis.keys(redisPrefix + "*").isEmpty());
    }

    @Test
    void databaseStillPreventsOverbookingWhenRedisMutualExclusionIsAbsent() throws Exception {
        // Models lease expiry/failover: both requests are admitted. Production never disables the gate.
        var dbOnly = service((users, event) -> () -> {});
        jdbc.update("UPDATE event SET register_limit=3 WHERE id=1");
        assertEquals(3, race(24, i -> dbOnly.add(i + 1L, 1L, 1L)));
        assertEquals(3, count("SELECT COUNT(*) FROM registration"));
    }

    @Test
    void sameUserOnDifferentEventsCannotExceedPersonalLimit() throws Exception {
        var dbOnly = service((users, event) -> () -> {});
        jdbc.update("UPDATE group_type SET per_person_limit=1,person_limit_event_ids='[1,2]' WHERE id=1");
        assertEquals(1, race(16, i -> dbOnly.add(1L, i % 2 + 1L, 1L)));
        assertEquals(1, count("SELECT COUNT(DISTINCT event_id) FROM registration WHERE participant_id=1"));
    }

    @Test
    void sameTeamCannotExceedConfiguredDistinctParticipantLimit() throws Exception {
        jdbc.update("UPDATE group_type SET per_team_limit=2,limit_event_ids='[1]' WHERE id=1");
        assertEquals(2, race(16, i -> (i % 2 == 0 ? first : second).add(i + 1L, 1L, 1L)));
        assertEquals(2, count("SELECT COUNT(DISTINCT participant_id) FROM registration"));
    }

    @Test
    void simultaneousFirstRegistrationRequeriesParticipantAndDoesNotDuplicate() throws Exception {
        jdbc.update("DELETE FROM participant WHERE user_id=1");
        assertEquals(1, race(16, i -> {
            Participant snapshot = new Participant();
            snapshot.setUserId(1L); snapshot.setSportsMeetingId(1L);
            (i % 2 == 0 ? first : second).add(snapshot, 1L, 1L);
        }));
        assertEquals(1, count("SELECT COUNT(*) FROM participant WHERE user_id=1"));
        assertEquals(1, count("SELECT COUNT(*) FROM registration"));
    }

    private void failInsert(RegistrationServiceImpl target, int failAt) {
        RegistrationMapper delegate = session.getMapper(RegistrationMapper.class);
        var calls = new AtomicInteger();
        var failing = Proxy.newProxyInstance(RegistrationMapper.class.getClassLoader(), new Class[]{RegistrationMapper.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("insert") && calls.incrementAndGet() == failAt) {
                        throw new IllegalStateException("injected insert failure");
                    }
                    try { return method.invoke(delegate, args); }
                    catch (InvocationTargetException e) { throw e.getCause(); }
                });
        ReflectionTestUtils.setField(target, "registrationMapper", failing);
    }

    @Test
    void failedDatabaseWriteRollsBackParticipantAndReleasesRedis() {
        jdbc.update("DELETE FROM participant WHERE user_id=1");
        var failing = service(gate()); failInsert(failing, 1);
        Participant snapshot = new Participant(); snapshot.setUserId(1L); snapshot.setSportsMeetingId(1L);
        assertEquals("injected insert failure", assertThrows(IllegalStateException.class,
                () -> failing.add(snapshot, 1L, 1L)).getMessage());
        assertEquals(0, count("SELECT COUNT(*) FROM participant WHERE user_id=1"));
        assertEquals(0, count("SELECT COUNT(*) FROM registration"));
        assertTrue(redis.keys(redisPrefix + "*").isEmpty());
        first.add(snapshot, 1L, 1L);
        assertEquals(1, count("SELECT COUNT(*) FROM registration"));
    }

    @Test
    void cancellationReleasesCapacityAndReactivationChecksLimits() {
        jdbc.update("UPDATE event SET register_limit=1 WHERE id=1");
        first.add(1L, 1L, 1L); Long original = registrationId(1);
        first.update(original, 2);
        second.add(2L, 1L, 1L);
        assertThrows(RuntimeException.class, () -> first.update(original, 0));
        assertEquals(2, jdbc.queryForObject("SELECT status FROM registration WHERE id=?", Integer.class, original));
        second.delete(registrationId(2));
        first.add(1L, 1L, 1L);
        assertEquals(original, registrationId(1));
        assertEquals(1, count("SELECT COUNT(*) FROM registration"));
    }

    @Test
    void promotionDoesNotCountSameTeamMemberTwiceAndRestorationDoesNotCountItself() {
        jdbc.update("UPDATE group_type SET per_team_limit=1,limit_event_ids='[1]',per_person_limit=1,person_limit_event_ids='[1,2]' WHERE id=1");
        jdbc.update("UPDATE event SET register_limit=1 WHERE id=1");
        first.add(1L, 1L, 1L); Long original = registrationId(1);
        first.update(original, 1);
        assertEquals(1, count("SELECT COUNT(*) FROM registration WHERE schedule_id=2 AND status=0"));
        first.update(original, 1); // idempotent retry
        assertEquals(2, count("SELECT COUNT(*) FROM registration"));
        first.update(original, 0);
        assertEquals(1, count("SELECT COUNT(*) FROM registration"));
        assertEquals(0, jdbc.queryForObject("SELECT status FROM registration WHERE id=?", Integer.class, original));
    }

    @Test
    void batchPromotionRollsBackAllSelectedParticipantsWhenLaterInsertFails() {
        first.add(1L, 1L, 1L); first.add(2L, 1L, 1L);
        jdbc.update("INSERT INTO result (sports_meeting_id,event_id,event_schedule_id,participant_id,score_value,points) "
                + "VALUES (1,1,1,1,100,0),(1,1,1,2,200,0)");
        var failing = service(gate()); failInsert(failing, 2);
        assertThrows(IllegalStateException.class, () -> failing.promoteTopN(1L, 1L, 2));
        assertEquals(2, count("SELECT COUNT(*) FROM registration WHERE schedule_id=1 AND status=0"));
        assertEquals(0, count("SELECT COUNT(*) FROM registration WHERE schedule_id=2"));
        assertEquals(2, first.promoteTopN(1L, 1L, 2));
        assertEquals(0, first.promoteTopN(1L, 1L, 2));
    }

    @Test
    void oldLeaseCannotDeleteNewOwnerAndContentionLeavesNoPartialLock() throws Exception {
        var shortGate = new RedisRegistrationGate(redis, redisPrefix, 80, 0);
        var old = shortGate.acquire(List.of(1L), 1L);
        // Contention on the event must not leave an acquired user:2 key.
        assertThrows(IllegalStateException.class, () -> shortGate.acquire(List.of(2L), 1L));
        assertFalse(redis.hasKey(redisPrefix + "{registration}:user:2"));
        Thread.sleep(120);
        try (var current = gate().acquire(List.of(1L), 1L)) {
            old.close();
            assertTrue(redis.hasKey(redisPrefix + "{registration}:event:1"));
            assertTrue(redis.hasKey(redisPrefix + "{registration}:user:1"));
        }
    }

    @Test
    void closedRegistrationWindowIsRecheckedInsideTransaction() {
        jdbc.update("UPDATE sports_meeting SET registration_end=NOW()-INTERVAL 1 MINUTE WHERE id=1");
        Participant p = new Participant(); p.setUserId(1L); p.setSportsMeetingId(1L);
        assertEquals("报名已截止", assertThrows(RuntimeException.class, () -> first.add(p, 1L, 1L)).getMessage());
        assertEquals(0, count("SELECT COUNT(*) FROM registration"));
    }
}
