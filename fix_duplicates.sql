-- 1. Check for duplicates
SELECT member_id, COUNT(*) 
FROM fcm_token 
GROUP BY member_id 
HAVING COUNT(*) > 1;

-- 2. Identify the specific rows for the problematic member (e.g., 11)
SELECT * FROM fcm_token WHERE member_id = 11;

-- 3. Delete duplicates, keeping the most recent one (assuming higher ID is more recent)
DELETE FROM fcm_token
WHERE id IN (
    SELECT id
    FROM (
        SELECT id, ROW_NUMBER() OVER (PARTITION BY member_id ORDER BY id DESC) as rn
        FROM fcm_token
    ) t
    WHERE t.rn > 1
);

-- 4. Verify cleanup
SELECT member_id, COUNT(*) 
FROM fcm_token 
GROUP BY member_id;
