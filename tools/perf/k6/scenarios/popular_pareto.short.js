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

function pickProductIdPareto() {
  if (Math.random() < 0.8) return randInt(1, 20000);
  return randInt(20001, 100000);
}

export default function () {
  const userId = pickUserId();

  const res1 = http.get(`${BASE_URL}/api/v1/products/popular?page=1&size=20`);
  check(res1, { 'GET /products/popular 200': (r) => r.status === 200 });

  if (Math.random() < 0.4) {
    const page = randInt(1, 3);
    const res2 = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=20`);
    check(res2, { 'GET /products 200': (r) => r.status === 200 });
  }

  if (Math.random() < 0.15) {
    const headers = { 'Content-Type': 'application/json', 'userId': String(userId) };
    const body = JSON.stringify({ orderItems: [{ productId: pickProductIdPareto(), quantity: 1 }], userCouponId: null });
    const r3 = http.post(`${BASE_URL}/api/v1/orders`, body, { headers });
    check(r3, { 'POST /orders 201/400/409': (r) => [201, 400, 409].includes(r.status) });
  }

  if (Math.random() < 0.5) {
    const headers = { 'userId': String(userId) };
    const r4 = http.get(`${BASE_URL}/api/v1/point`, { headers });
    check(r4, { 'GET /point 200': (r) => r.status === 200 });
  }

  sleep(1);
}



