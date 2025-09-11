import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: Number(__ENV.VU_START || 5) },
    { duration: '8m', target: Number(__ENV.VU_END || 50) },
    { duration: '2m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function randInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }
function pickUserId() { return randInt(1, 10000); }
function pickProductId() { return randInt(1, 100000); }
function pickCouponId() { return randInt(1, 10); }

export default function () {
  const userId = pickUserId();

  // Read-heavy mix
  if (Math.random() < 0.7) {
    const page = randInt(1, 5);
    const size = randInt(10, 20);
    const res = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=${size}`);
    check(res, { 'GET /products 200': (r) => r.status === 200 });
  } else {
    const res = http.get(`${BASE_URL}/api/v1/products/popular?page=1&size=10`);
    check(res, { 'GET /products/popular 200': (r) => r.status === 200 });
  }

  // Light writes
  if (Math.random() < 0.15) {
    const headers = { 'Content-Type': 'application/json', 'userId': String(userId) };
    const amount = randInt(100, 1000);
    const res = http.patch(`${BASE_URL}/api/v1/point/charge`, JSON.stringify({ amount }), { headers });
    check(res, { 'PATCH /point/charge 201/204': (r) => [201, 204].includes(r.status) });
  }
  if (Math.random() < 0.1) {
    const headers = { 'Content-Type': 'application/json', 'userId': String(userId) };
    const items = Array.from({ length: randInt(1, 3) }, () => ({ productId: pickProductId(), quantity: randInt(1, 3) }));
    const res = http.post(`${BASE_URL}/api/v1/orders`, JSON.stringify({ orderItems: items, userCouponId: null }), { headers });
    check(res, { 'POST /orders 201/400/409': (r) => [201, 400, 409].includes(r.status) });
  }

  sleep(1);
}


