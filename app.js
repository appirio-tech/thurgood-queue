var kue = require('kue');
var url = require('url');

if (process.env.REDIS_URL) {
  var redisURL = url.parse(process.env.REDIS_URL);
  var queue = kue.createQueue({
    prefix: 'q',
    redis: {
      port: redisURL.port,
      host: redisURL.hostname,
      auth: redisURL.auth.split(":")[1]
    }
  });
} else {
  var queue = kue.createQueue();
}

kue.app.listen(process.env.PORT || 3001);
kue.app.set('title', 'Thurgood Queue');
