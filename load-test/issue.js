import http from 'k6/http';
import exec from 'k6/execution';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { BASE_URL, MEMBER_COUNT, jwtFor, issuePath, TARGET } from './lib/config.js';

// 선착순 발급 동시성 시나리오.
// shared-iterations: 총 REQUESTS건을 VUS 동시성으로 쏜다. iterationInTest로 회원을 전부 다르게 배정해
// 재고보다 많은 서로 다른 회원이 동시에 경합하도록 만든다(오버셀 재현용).
//
// 측정:
//  - issue_success(200): 발급 성공 수
//  - issue_soldout(409 CP003): 품절 거절 수
//  - issue_duplicate(409 CP002): 중복 거절 수
//  - issue_error(그 외): 예기치 못한 응답(401/500 등) = 정합성/설정 문제 신호
// 정확 발급(after)이면 success == 재고, 오버셀 0. naive면 success > 재고(오버셀)가 관측된다.

const successC = new Counter('issue_success');
const soldoutC = new Counter('issue_soldout');
const dupC = new Counter('issue_duplicate');
const errorC = new Counter('issue_error');

const VUS = parseInt(__ENV.VUS || '200', 10);
const REQUESTS = parseInt(__ENV.REQUESTS || String(MEMBER_COUNT), 10);

export const options = {
    scenarios: {
        spike: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: REQUESTS,
            maxDuration: __ENV.MAX_DURATION || '20m',
        },
    },
    thresholds: {
        // 200/409 외 응답(예기치 못한 에러)은 거의 없어야 한다.
        issue_error: ['count<' + (__ENV.ERROR_BUDGET || '1')],
        http_req_duration: ['p(95)<' + (__ENV.P95_MS || '5000')],
    },
};

const PATH = issuePath();

export function setup() {
    console.log(`[setup] TARGET=${TARGET} path=${PATH} VUS=${VUS} REQUESTS=${REQUESTS} BASE_URL=${BASE_URL}`);
}

export default function () {
    const memberId = (exec.scenario.iterationInTest % MEMBER_COUNT) + 1;
    const res = http.post(`${BASE_URL}${PATH}`, null, {
        headers: { Authorization: `Bearer ${jwtFor(memberId)}` },
    });

    if (res.status === 200) {
        successC.add(1);
    } else if (res.status === 409) {
        let code = '';
        try {
            code = res.json('error.code');
        } catch (e) {
            // 응답 본문이 JSON이 아니면 품절로 집계
        }
        if (code === 'CP002') {
            dupC.add(1);
        } else {
            soldoutC.add(1);
        }
    } else {
        errorC.add(1);
    }

    check(res, { 'status is 200 or 409': (r) => r.status === 200 || r.status === 409 });
}
