-- 테이블은 JPA가 자동으로 생성하므로 INSERT 문만 작성합니다.

-- 1. 도둑 등급
INSERT INTO grade_thief (id, name, created_at, updated_at, is_deleted)
VALUES
    (1, '바늘도둑', NOW(), NOW(), false),
    (2, '좀도둑', NOW(), NOW(), false),
    (3, '소매치기', NOW(), NOW(), false),
    (4, '빈집털이', NOW(), NOW(), false),
    (5, '소도둑', NOW(), NOW(), false),
    (6, '금고털이', NOW(), NOW(), false),
    (7, '은행털이', NOW(), NOW(), false),
    (8, '홍길동', NOW(), NOW(), false),
    (9, '인비져블', NOW(), NOW(), false),
    (10, '괴도', NOW(), NOW(), false),
    (11, '대도', NOW(), NOW(), false)
    ON CONFLICT (id) DO UPDATE
                            SET name = EXCLUDED.name;

-- 2. 경찰 등급
INSERT INTO grade_police (id, name, created_at, updated_at, is_deleted)
VALUES
    (1, '순경', NOW(), NOW(), false),
    (2, '경장', NOW(), NOW(), false),
    (3, '경사', NOW(), NOW(), false),
    (4, '경위', NOW(), NOW(), false),
    (5, '경감', NOW(), NOW(), false),
    (6, '경정', NOW(), NOW(), false),
    (7, '총경', NOW(), NOW(), false),
    (8, '경무관', NOW(), NOW(), false),
    (9, '치안감', NOW(), NOW(), false),
    (10, '치안정감', NOW(), NOW(), false),
    (11, '치안총감', NOW(), NOW(), false)
    ON CONFLICT (id) DO UPDATE
                            SET name = EXCLUDED.name;