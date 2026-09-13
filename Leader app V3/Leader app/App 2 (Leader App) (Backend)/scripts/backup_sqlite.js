const path = require('path');
const fs = require('fs');

const appDir = path.resolve(__dirname, '..');
const dbPath = path.join(appDir, 'data', 'leader.db');
const backupDbPath = path.join(appDir, 'data', 'leader.db.bak');
const backupJsonPath = path.join(appDir, 'data', 'backup_leader_sqlite.json');

// Copy binary database file
if (fs.existsSync(dbPath)) {
  fs.copyFileSync(dbPath, backupDbPath);
  console.log(`Binary backup created at: ${backupDbPath}`);
}

// Export all tables to JSON
const Database = require(path.join(appDir, 'node_modules', 'better-sqlite3'));
const db = new Database(dbPath);

const tables = db.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'knex_%'").all();

const fullData = {};
for (const table of tables) {
  const rows = db.prepare(`SELECT * FROM "${table.name}"`).all();
  fullData[table.name] = rows;
  console.log(`Exported ${rows.length} records from table: ${table.name}`);
}

fs.writeFileSync(backupJsonPath, JSON.stringify(fullData, null, 2), 'utf-8');
console.log(`JSON backup successfully created at: ${backupJsonPath}`);
