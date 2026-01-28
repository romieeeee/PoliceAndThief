import * as turf from "@turf/turf";

export class TurfService {
    checkUserInBoundary = async (userPoint, boundary) => {
        const userPt = turf.point(userPoint);

        // 경계선의 처음과 끝이 같은지 확인 (LinearRing 조건)
        const start = boundary[0];
        const end = boundary[boundary.length - 1];
        if (start[0] !== end[0] || start[1] !== end[1]) {
            boundary.push(start);
        }

        // turf.polygon은 [[[x,y], [x,y], ...]] 형태의 3중 배열을 받습니다 (첫번째 링이 외곽선)
        const boundaryPt = turf.polygon([boundary]);

        const userErrorRange = turf.buffer(userPt, 5, { units: 'meters' });

        // 2. 유저의 오차 범위 원과 경계선(Boundary)이 겹치는지 확인
        // booleanIntersects는 두 도형이 조금이라도 닿아있으면 true를 반환합니다.
        const isOverlapping = turf.booleanIntersects(userErrorRange, boundaryPt);

        return isOverlapping;
    }

    checkUserInPrison = async (userPoint, prisonLocation) => {
        const userPt = turf.point(userPoint);

        const prisonPt = turf.point(prisonLocation);

        const distance = turf.distance(userPt, prisonPt, { units: 'meters' });

        console.log("distance", distance);

        return distance <= 15;
    }
}
