const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const helpers = require('../sportdocu/utils/match')

const event = (id, extra = {}) => ({ id, name: '100米', gender: '男', category: '径赛',
  groupTypeId: 1, groupTypeName: '本科组', ...extra })
const meetings = [{ id: 1, status: 2, name: '春季运动会' },
  { id: 2, status: 2, name: '秋季运动会' }, { id: 3, status: 3, name: '历史运动会' }]
const tick = () => new Promise(resolve => setImmediate(resolve))
function deferred() {
  let resolve, reject
  const promise = new Promise((yes, no) => { resolve = yes; reject = no })
  return { promise, resolve, reject }
}
function createPage(request) {
  let definition
  const wx = { stopPullDownRefresh() {}, navigateTo() {} }
  vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../sportdocu/pages/match/match.js'), 'utf8'), {
    Page: page => { definition = page }, wx,
    require: name => name.endsWith('/auth') ? { request } : helpers,
  })
  const page = { ...definition, data: JSON.parse(JSON.stringify(definition.data)),
    setData(patch) { Object.assign(this.data, patch) } }
  page.onLoad()
  return page
}
function basicRequest(url) {
  if (url === '/api/sports-meeting/list') return Promise.resolve(meetings)
  if (url.startsWith('/api/schedule/')) return Promise.resolve([{ id: 1, name: '决赛', sort: 1 }])
  if (url.startsWith('/api/event/')) return Promise.resolve([event(10)])
  return Promise.resolve([])
}

test('group identity uses group IDs, safely handles delimiters and deduplicates event IDs', () => {
  const groups = helpers.groupEvents([event(1), event(1), event(2), event(3, { groupTypeId: 2 }),
    event(4, { name: '100|米' })])
  assert.equal(groups.length, 3)
  assert.deepEqual(groups[0].rounds.map(row => row.id), [1, 2])
  assert.equal(helpers.filterEvents(groups, '本科', '径赛', '2').length, 1)
  assert.equal(helpers.filterEvents(groups, '跳高', '', null).length, 0)
})

test('merged results sort within each round, preserve round order and leave missing scores unranked', () => {
  const rows = [
    { id: 1, scheduleId: 2, scoreValue: 14000 },
    { id: 2, scheduleId: 1, scoreValue: null },
    { id: 3, scheduleId: 2, scoreValue: 12000 },
    { id: 4, scheduleId: 1, scoreValue: 0 },
    { id: 5, scheduleId: null, scoreValue: 10000 },
  ]
  const rounds = helpers.buildResults(rows, [{ id: 1, name: '预赛', sort: 0 }, { id: 2, name: '决赛', sort: 2 }], '径赛')
  assert.deepEqual(rounds.map(round => round.scheduleName), ['预赛', '决赛', '未分轮次'])
  assert.deepEqual(rounds[1].rows.map(row => row.key), ['3', '1'])
  assert.equal(rounds[0].rows[0].score, '0.000秒')
  assert.equal(rounds[0].rows[1].rank, '')
  assert.equal(rounds[0].scoredCount, 1)
  assert.equal(rows[0].id, 1)
})

test('field scores sort descending; track and team scores format minutes without losing milliseconds', () => {
  const rounds = helpers.buildResults([{ id: 1, scoreValue: 620 }, { id: 2, scoreValue: 710 }], [], '田赛')
  assert.equal(rounds[0].rows[0].score, '7.10米')
  assert.equal(helpers.formatScore(61005, '团队赛'), '1分01.005秒')
  assert.equal(helpers.formatScore(12034, '径赛'), '12.034秒')
  assert.equal(helpers.formatScore(null, '田赛'), '待录入')
})

test('late responses from a previously selected meeting cannot replace the current meeting', async () => {
  const slow = deferred()
  const page = createPage(({ url }) => url === '/api/event/list?sportsMeetingId=1'
    ? slow.promise : basicRequest(url))
  const initial = page.onShow()
  await tick()
  await page.selectMeeting(2)
  slow.resolve([event(90, { name: '旧运动会项目' })])
  await initial
  assert.equal(page.data.activeMeetingId, 2)
  assert.equal(page.data.events[0].name, '100米')
  assert.equal(page.data.eventsLoading, false)
})

test('result requests are lazy, deduplicated and isolated per project; failures can retry', async () => {
  let calls = 0
  const pending = deferred()
  const page = createPage(({ url }) => {
    if (url.startsWith('/api/result/')) {
      calls++
      return calls === 1 ? pending.promise : Promise.resolve([{ id: 1, scoreValue: 12000 }])
    }
    return basicRequest(url)
  })
  await page.onShow()
  assert.equal(calls, 0)
  const key = page.data.events[0].key
  const first = page.loadResults(key)
  await page.loadResults(key)
  assert.equal(calls, 1)
  pending.reject(new Error('offline'))
  await first
  assert.equal(page.data.events[0].resultState, 'error')
  await page.loadResults(key)
  assert.equal(page.data.events[0].resultState, 'ready')
  assert.equal(page.data.events[0].results[0].rows[0].score, '12.000秒')
})

test('late result responses cannot contaminate an identically named project in another meeting', async () => {
  const slow = deferred()
  const page = createPage(({ url }) => url.startsWith('/api/result/') ? slow.promise : basicRequest(url))
  await page.onShow()
  const key = page.data.events[0].key
  const old = page.loadResults(key)
  await page.selectMeeting(2)
  slow.resolve([{ id: 1, scoreValue: 12000, participantName: '旧选手' }])
  await old
  assert.equal(page.data.events[0].resultState, 'idle')
  assert.equal(page.data.events[0].results.length, 0)
})

test('return and pull-to-refresh retain selection and filters and reload expanded scores', async () => {
  let score = 12000
  let calls = 0
  const page = createPage(({ url }) => {
    if (url.startsWith('/api/result/')) { calls++; return Promise.resolve([{ id: 1, scoreValue: score }]) }
    return basicRequest(url)
  })
  await page.onShow()
  await page.selectMeeting(2)
  page.onSearchInput({ detail: { value: '100' } })
  page.onCategoryTap({ currentTarget: { dataset: { category: '径赛' } } })
  await page.onEventTap({ currentTarget: { dataset: { key: page.data.events[0].key } } })
  score = 11000
  await page.onShow()
  assert.equal(page.data.activeMeetingId, 2)
  assert.equal(page.data.keyword, '100')
  assert.equal(page.data.category, '径赛')
  assert.equal(page.data.events[0].results[0].rows[0].score, '11.000秒')
  await page.onPullDownRefresh()
  assert.equal(calls, 3)
})

test('historical switch invalidates pending event requests and loads only finished meetings', async () => {
  const slow = deferred()
  const page = createPage(({ url }) => url === '/api/event/list?sportsMeetingId=1' ? slow.promise : basicRequest(url))
  const initial = page.onShow()
  await tick()
  await page.onStatusTap({ currentTarget: { dataset: { status: 3 } } })
  slow.resolve([event(99, { name: '旧项目' })])
  await initial
  assert.equal(page.data.activeMeetingId, 3)
  assert.equal(page.data.meetingOptions.length, 1)
  assert.equal(page.data.events[0].name, '100米')
})

test('schedule failures retain usable projects and show a retryable warning', async () => {
  const page = createPage(({ url }) => url.startsWith('/api/schedule/')
    ? Promise.reject(new Error('offline')) : basicRequest(url))
  await page.onShow()
  assert.equal(page.data.events.length, 1)
  assert.ok(page.data.scheduleError)
  assert.equal(page.data.eventsError, '')
})

test('results wait for slow schedule metadata so rounds follow configured order', async () => {
  const schedules = deferred()
  let resultCalls = 0
  const page = createPage(({ url }) => {
    if (url.startsWith('/api/schedule/')) return schedules.promise
    if (url.startsWith('/api/result/')) {
      resultCalls++
      return Promise.resolve([{ id: 1, scheduleId: 2, scoreValue: 10000 },
        { id: 2, scheduleId: 1, scoreValue: 11000 }])
    }
    return basicRequest(url)
  })
  const loading = page.onShow()
  await tick()
  assert.equal(page.data.eventsLoading, true)
  assert.equal(resultCalls, 0)
  schedules.resolve([{ id: 1, sort: 0, name: '预赛' }, { id: 2, sort: 1, name: '决赛' }])
  await loading
  await page.onEventTap({ currentTarget: { dataset: { key: page.data.events[0].key } } })
  assert.deepEqual(page.data.events[0].results.map(round => round.scheduleName), ['预赛', '决赛'])
})

test('a failed request in a merged project never presents partial results as a full ranking', async () => {
  let fail = true
  const page = createPage(({ url }) => {
    if (url.startsWith('/api/event/')) return Promise.resolve([event(10), event(20)])
    if (url.endsWith('eventId=10')) return Promise.resolve([{ id: 1, scoreValue: 12000 }])
    if (url.endsWith('eventId=20')) return fail ? Promise.reject(new Error('offline'))
      : Promise.resolve([{ id: 2, scoreValue: 11000 }])
    return basicRequest(url)
  })
  await page.onShow()
  const key = page.data.events[0].key
  await page.loadResults(key)
  assert.equal(page.data.events[0].resultState, 'error')
  assert.equal(page.data.events[0].results.length, 0)
  fail = false
  await page.loadResults(key)
  assert.deepEqual(page.data.events[0].results[0].rows.map(row => row.score), ['11.000秒', '12.000秒'])
})

test('meeting failures are different from empty lists and unload suppresses late updates', async () => {
  const page = createPage(() => Promise.reject(new Error('offline')))
  await page.onShow()
  assert.ok(page.data.pageError)
  assert.equal(page.data.loading, false)
  const pending = deferred()
  const unloaded = createPage(() => pending.promise)
  const work = unloaded.onShow()
  unloaded.onUnload()
  unloaded.setData = () => assert.fail('updated after unload')
  pending.resolve(meetings)
  await work
})
