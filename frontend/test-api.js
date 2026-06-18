const api = require('./lib/api')
api.getCourse(1).then(console.log).catch(console.error)
