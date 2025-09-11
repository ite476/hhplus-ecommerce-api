import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VU || 10),
  duration: __ENV.DURATION || '60s',
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<600'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function randInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }
function pickUserId() { return randInt(1, 10000); }

export default function () {
  const userId = pickUserId();

  const page = randInt(1, 3);
  const res = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=20`);
  check(res, { 'GET /products 200': (r) => r.status === 200 });

  if (Math.random() < 0.2) {
    const headers = { 'Content-Type': 'application/json', 'userId': String(userId) };
    const body = JSON.stringify({ orderItems: [{ productId: 1, quantity: 1 }], userCouponId: null });
    const r2 = http.post(`${BASE_URL}/api/v1/orders`, body, { headers });
    check(r2, { 'POST /orders 201/400/409': (r) => [201, 400, 409].includes(r.status) });
  }

  sleep(1);
}



