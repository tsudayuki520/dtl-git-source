// 赛况展示的数据整理，与页面生命周期、网络请求分离。
function groupEvents(events) {
  const map = new Map()
  events.forEach(event => {
    const key = JSON.stringify([event.name, event.gender, event.category,
      event.groupTypeId == null ? event.groupTypeName : event.groupTypeId])
    if (!map.has(key)) {
      map.set(key, {
        key, name: event.name, gender: event.gender || '不限', category: event.category,
        groupKey: String(event.groupTypeId == null ? (event.groupTypeName || '') : event.groupTypeId),
        groupTypeName: event.groupTypeName || '未分组', rounds: [],
        results: [], resultState: 'idle', resultError: '',
      })
    }
    const group = map.get(key)
    if (!group.rounds.some(round => round.id === event.id)) group.rounds.push(event)
  })
  return Array.from(map.values())
}

function filterEvents(events, keyword, category, groupKey) {
  const query = keyword.trim().toLowerCase()
  return events.filter(event => (!category || event.category === category)
    && (groupKey === null || event.groupKey === groupKey)
    && (!query || [event.name, event.gender, event.groupTypeName].join(' ').toLowerCase().includes(query)))
}

function formatScore(value, category) {
  if (value == null) return '待录入'
  if (category === '田赛') return (value / 100).toFixed(2) + '米'
  if (category !== '径赛' && category !== '团队赛') return String(value)
  const minutes = Math.floor(value / 60000)
  const seconds = Math.floor(value / 1000) % 60
  const millis = String(value % 1000).padStart(3, '0')
  return minutes > 0
    ? minutes + '分' + String(seconds).padStart(2, '0') + '.' + millis + '秒'
    : seconds + '.' + millis + '秒'
}

function buildResults(rows, schedules, category) {
  const scheduleMap = new Map(schedules.map(schedule => [String(schedule.id), schedule]))
  const rounds = new Map()
  rows.forEach(row => {
    const key = row.scheduleId == null ? 'none' : String(row.scheduleId)
    if (!rounds.has(key)) {
      const schedule = scheduleMap.get(key)
      rounds.set(key, {
        key, scheduleName: (schedule && schedule.name) || row.scheduleName || '未分轮次',
        sort: schedule && schedule.sort != null ? schedule.sort : Number.MAX_SAFE_INTEGER,
        rows: [],
      })
    }
    rounds.get(key).rows.push(row)
  })
  return Array.from(rounds.values()).sort((a, b) => a.sort - b.sort).map(round => {
    // 不同 event 的已排序结果拼接后必须再排序；未录入成绩始终在末尾。
    round.rows.sort((a, b) => {
      if (a.scoreValue == null) return b.scoreValue == null ? 0 : 1
      if (b.scoreValue == null) return -1
      return category === '田赛' ? b.scoreValue - a.scoreValue : a.scoreValue - b.scoreValue
    })
    return {
      key: round.key, scheduleName: round.scheduleName,
      scoredCount: round.rows.filter(row => row.scoreValue != null).length,
      rows: round.rows.map((row, index) => ({
        key: row.id == null ? round.key + '_' + index : String(row.id),
        rank: row.scoreValue == null ? '' : index + 1,
        name: row.participantName || '未命名选手', unit: row.college || '',
        score: formatScore(row.scoreValue, row.category || category),
        hasScore: row.scoreValue != null, points: row.points == null ? 0 : row.points,
      })),
    }
  })
}

module.exports = { groupEvents, filterEvents, formatScore, buildResults }
