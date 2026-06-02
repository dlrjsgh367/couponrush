import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

// 부하테스트 공통 설정. 값은 환경변수로 덮어쓴다.
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const EVENT_ID = __ENV.EVENT_ID || '1';
export const TARGET = __ENV.TARGET || 'after'; // after(Redis 경로) | naive(개선 전 DB-only)
export const MEMBER_COUNT = parseInt(__ENV.MEMBER_COUNT || '200000', 10);

// 앱과 동일한 JWT 서명 키. 앱의 jwt.secret(application.yml)과 반드시 일치해야 한다.
const SECRET = __ENV.JWT_SECRET || 'couponrush-local-dev-secret-key-please-change-on-server-256bit';

function b64url(input) {
    return encoding.b64encode(input, 'rawurl');
}

// memberId로 앱과 호환되는 HS256 JWT를 즉석 발급한다(로그인 호출 없이 토큰 확보).
// 발급 INSERT는 members FK를 타므로 해당 memberId의 회원 row가 미리 시드돼 있어야 한다.
export function jwtFor(memberId) {
    const header = b64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const now = Math.floor(Date.now() / 1000);
    const payload = b64url(JSON.stringify({
        sub: String(memberId),
        email: `load${memberId}@couponrush.com`,
        iat: now,
        exp: now + 3600,
    }));
    const signingInput = `${header}.${payload}`;
    const sig = crypto.hmac('sha256', SECRET, signingInput, 'base64rawurl');
    return `${signingInput}.${sig}`;
}

export function issuePath() {
    return TARGET === 'naive'
        ? `/api/coupons/${EVENT_ID}/issue-naive`
        : `/api/coupons/${EVENT_ID}/issue`;
}
