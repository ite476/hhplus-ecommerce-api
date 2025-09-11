import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VU || 1),
  duration: __ENV.DURATION || '10s',
  thresholds: {
    http_req_failed: ['rate<0.2'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/products?page=1&size=5`);
  check(res, { 'GET /products 200': (r) => r.status === 200 });
  sleep(1);
}


