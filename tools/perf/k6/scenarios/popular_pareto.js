import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VU || 10),
  duration: __ENV.DURATION || '5m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<600'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

function randInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function pickUserId() {
  // users.bulk-insert.sql 기준: 1..10000
  return randInt(1, 10000);
}

function pickProductIdPareto() {
  // Pareto 근사: 상위 20% 상품(1..20000)에 80% 트래픽 집중
  if (Math.random() < 0.8) {
    return randInt(1, 20000);
  }
  return randInt(20001, 100000);
}

export default function () {
  const userId = pickUserId();

  // 인기 Product 목록 위주 접근
  {
    const res = http.get(`${BASE_URL}/api/v1/products/popular?page=1&size=20`);
    check(res, { 'GET /products/popular 200': (r) => r.status === 200 });
  }

  // Product 목록 일부
  if (Math.random() < 0.4) {
    const page = randInt(1, 3);
    const size = 20;
    const res = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=${size}`);
    check(res, { 'GET /products 200': (r) => r.status === 200 });
  }

  // 주문 생성: Pareto 분포로 상품 선택
  if (Math.random() < 0.15) {
    const headers = { 'Content-Type': 'application/json', 'userId': String(userId) };
    const itemsCount = randInt(1, 3);
    const orderItems = Array.from({ length: itemsCount }, () => ({
      productId: pickProductIdPareto(),
      quantity: randInt(1, 3),
    }));
    const body = JSON.stringify({ orderItems, userCouponId: null });
    const res = http.post(`${BASE_URL}/api/v1/orders`, body, { headers });
    check(res, {
      'POST /orders 201/400/409': (r) => [201, 400, 409].includes(r.status),
    });
  }

  // 포인트 조회(가벼운 read 혼합)
  if (Math.random() < 0.5) {
    const headers = { 'userId': String(userId) };
    const res = http.get(`${BASE_URL}/api/v1/point`, { headers });
    check(res, { 'GET /point 200': (r) => r.status === 200 });
  }

  sleep(1);
}


