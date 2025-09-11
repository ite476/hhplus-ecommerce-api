import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    same_product_orders: {
      executor: 'constant-vus', vus: Number(__ENV.VU || 20), duration: __ENV.DURATION || '60s', exec: 'orderSameProduct',
    },
    same_coupon_issue: {
      executor: 'constant-vus', vus: Number(__ENV.VU_COUPON || 10), duration: __ENV.DURATION || '60s', exec: 'issueSameCoupon',
    },
  },
  thresholds: { http_req_failed: ['rate<0.05'] },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const HOT_PRODUCT_ID = Number(__ENV.HOT_PRODUCT_ID || 1);
const HOT_COUPON_ID = Number(__ENV.HOT_COUPON_ID || 1);

function randInt(min, max) { return Math.floor(Math.random() * (max - min + 1)) + min; }

export function orderSameProduct() {
  const headers = { 'Content-Type': 'application/json', 'userId': String(randInt(1, 10000)) };
  const body = JSON.stringify({ orderItems: [{ productId: HOT_PRODUCT_ID, quantity: 1 }], userCouponId: null });
  const res = http.post(`${BASE_URL}/api/v1/orders`, body, { headers });
  check(res, { 'POST /orders 201/400/409': (r) => [201, 400, 409].includes(r.status) });
  sleep(0.2);
}

export function issueSameCoupon() {
  const headers = { 'userId': String(randInt(1, 10000)) };
  const res = http.post(`${BASE_URL}/api/v1/coupons/${HOT_COUPON_ID}`, null, { headers });
  check(res, { 'POST /coupons/{id} 2xx/409': (r) => [200, 201, 202, 204, 409].includes(r.status) });
  sleep(0.2);
}



