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