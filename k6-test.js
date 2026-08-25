import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  // Simulates 50 concurrent virtual users for 10 seconds
  vus: 50,
  duration: '10s',
  thresholds: {
    // 95% of requests must respond in less than 10ms (Powered by Redis Cache!)
    http_req_duration: ['p(95)<10'],
    // 100% of requests must succeed
    http_req_failed: ['rate==0'],
  },
};

export default function () {
  // Tests redirect of shortCode "1" without following redirect to measure backend response time
  const res = http.get('http://host.docker.internal:8080/a', {
    redirects: 0,
  });

  check(res, {
    'status is 302': (r) => r.status === 302,
  });

  sleep(0.01); // 10ms pacing per virtual user
}