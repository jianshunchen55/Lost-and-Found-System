const mysql = require('mysql2/promise')

async function tryConnect(opts) {
  const label = `${opts.host}:${opts.port} user=${opts.user} password=${opts.password} db=${opts.database}`
  try {
    const conn = await mysql.createConnection({
      host: opts.host,
      port: opts.port,
      user: opts.user,
      password: opts.password,
      database: opts.database
    })
    const [rows] = await conn.query('SELECT 1 AS ok')
    await conn.end()
    return { label, success: true, rows }
  } catch (e) {
    return { label, success: false, error: e.message }
  }
}

;(async () => {
  const candidates = [
    { host: '127.0.0.1', port: 3306, user: 'root', password: 'root', database: 'lostfound' },
    { host: '127.0.0.1', port: 3306, user: 'root', password: '123456', database: 'lostfound' }
  ]
  const results = []
  for (const c of candidates) {
    const r = await tryConnect(c)
    results.push(r)
    console.log(`[${r.success ? 'SUCCESS' : 'FAIL'}] ${r.label} ${r.success ? JSON.stringify(r.rows) : r.error}`)
  }
  const ok = results.find(r => r.success)
  if (!ok) {
    process.exitCode = 1
  }
})()
