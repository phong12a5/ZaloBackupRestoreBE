db = db.getSiblingDB('authdb');
db.createUser({
  user: 'admin',
  pwd: 'password',
  roles: [{ role: 'readWrite', db: 'authdb' }]
});

db = db.getSiblingDB('userdb');
db.createUser({
  user: 'admin',
  pwd: 'password',
  roles: [{ role: 'readWrite', db: 'userdb' }]
});

// Add user for backupdb
db = db.getSiblingDB('backupdb');
db.createUser({
  user: 'admin', // Or a dedicated user like 'backup_user'
  pwd: 'password',
  roles: [{ role: 'readWrite', db: 'backupdb' }]
});