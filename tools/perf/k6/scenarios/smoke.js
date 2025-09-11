import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: Number(__ENV.VU || 5),
  duration: __ENV.DURATION || '2m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
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

function pickProductId() {
  // products.bulk-insert.sql 기준: 1..100000 (균등)
  return randInt(1, 100000);
}

function pickCouponId() {
  // coupon.insert.sql 기준: 1..10
  return randInt(1, 10);
}

export default function () {
  const userId = pickUserId();

  // 1) Product 목록
  {
    const page = randInt(1, 5);
    const size = randInt(10, 20);
    const res = http.get(`${BASE_URL}/api/v1/products?page=${page}&size=${size}`);
    check(res, {
      'GET /products status is 200': (r) => r.status === 200,
    });
  }

  // 2) 인기 Product 목록
  {
    const page = 1;
    const size = 10;
    const res = http.get(`${BASE_URL}/api/v1/products/popular?page=${page}&size=${size}`);
    check(res, {
      'GET /products/popular status is 200': (r) => r.status === 200,
    });
  }

  // 3) 포인트 조회
  {
    const headers = { 'userId': String(userId) };
    const res = http.get(`${BASE_URL}/api/v1/point`, { headers });
    check(res, {
      'GET /point status is 200': (r) => r.status === 200,
    });
  }

  // 4) 쿠폰 조회
  {
    const headers = { 'userId': String(userId) };
    const res = http.get(`${BASE_URL}/api/v1/mycoupons`, { headers });
    check(res, {
      'GET /mycoupons status is 200': (r) => r.status === 200,
    });
  }

  // 5) 쿠폰 발급 (일부 사용자만 시도)
  if (Math.random() < 0.1) {
    const headers = { 'userId': String(userId) };
    const couponId = pickCouponId();
    const res = http.post(`${BASE_URL}/api/v1/coupons/${couponId}`, null, { headers });
    check(res, {
      'POST /coupons/{id} status is 2xx/409': (r) => [200, 201, 202, 204, 409].includes(r.status),
    });
  }

  // 6) 포인트 충전/사용 (확률적으로)
  if (Math.random() < 0.2) {
    const headers = { 'Content-Type': 'application/json', 'userId': String(userId) };
    const amount = randInt(100, 1000);
    const res = http.patch(`${BASE_URL}/api/v1/point/charge`, JSON.stringify({ amount }), { headers });
    check(res, {
      'PATCH /point/charge status is 201/204': (r) => [201, 204].includes(r.status),
    });
  }
  if (Math.random() < 0.2) {
    const headers = { 'Content-Type': 'application/json', 'userId': String(userId) };
    const amount = randInt(50, 300);
    const res = http.patch(`${BASE_URL}/api/v1/point/use`, JSON.stringify({ amount }), { headers });
    check(res, {
      'PATCH /point/use status is 201/204/400': (r) => [201, 204, 400].includes(r.status),
    });
  }

  // 7) 주문 생성 (낮은 확률)
  if (Math.random() < 0.05) {
    const headers = { 'Content-Type': 'application/json', 'userId': String(userId) };
    const itemsCount = randInt(1, 3);
    const orderItems = Array.from({ length: itemsCount }, () => ({
      productId: pickProductId(),
      quantity: randInt(1, 3),
    }));
    const body = JSON.stringify({ orderItems, userCouponId: null });
    const res = http.post(`${BASE_URL}/api/v1/orders`, body, { headers });
    check(res, {
      'POST /orders status is 201/400/409': (r) => [201, 400, 409].includes(r.status),
    });
  }

  sleep(1);
}


