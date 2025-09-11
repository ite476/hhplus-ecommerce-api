import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 20 },
    { duration: '3m', target: 50 },
    { duration: '3m', target: 100 },
    { duration: '3m', target: 200 },
    { duration: '3m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<800'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function randInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }
function pickUserId() { return randInt(1, 10000); }
function pickProductId() { return randInt(1, 100000); }

export default function () {
  const userId = pickUserId();

  // Heavier mix of writes
  if (Math.random() < 0.5) {
    const headers = { 'Content-Type': 'application/json', 'userId': String(userId) };
    const items = Array.from({ length: randInt(1, 3) }, () => ({ productId: pickProductId(), quantity: randInt(1, 3) }));
    const res = http.post(`${BASE_URL}/api/v1/orders`, JSON.stringify({ orderItems: items, userCouponId: null }), { headers });
    check(res, { 'POST /orders 201/400/409': (r) => [201, 400, 409].includes(r.status) });
  } else {
    const page = randInt(1, 3);
    const res = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=20`);
    check(res, { 'GET /products 200': (r) => r.status === 200 });
  }

  sleep(0.5);
}


