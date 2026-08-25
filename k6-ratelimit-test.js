import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

// Custom metrics to track exact bucket enforcement
const createdCount = new Counter('urls_created_201');
const blockedCount = new Counter('rate_limited_429');

export const options = {
  // 1 single client sequentially firing 20 requests
  vus: 1,
  iterations: 20,
  thresholds: {
    // Mathematical proof: The bucket must allow exactly the first 10 requests!
    'urls_created_201': ['count==10'],
    // All 10 remaining requests must be blocked with HTTP 429!
    'rate_limited_429': ['count==10'],
    // Interceptor block must be evaluated in under 5ms
    'http_req_duration': ['p(95)<5'],
  },
};

export default function () {
  const url = 'http://host.docker.internal:8080/api/urls';
  const payload = JSON.stringify({
    url: 'https://google.com',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.post(url, payload, params);

  if (res.status === 201) {
    createdCount.add(1);
  } else if (res.status === 429) {
    blockedCount.add(1);
  }

  check(res, {
    'status is 201 (allowed) or 429 (rate-limited)': (r) => r.status === 201 || r.status === 429,
  });
}
