-- 修复私聊频道成员问题
-- 在 im_db 数据库中执行

-- 1. 查看所有私聊频道的成员情况
SELECT c.id as channel_id, c.channel_type, c.member_count, 
       array_agg(cm.user_id) as actual_members,
       count(cm.user_id) as actual_count
FROM channels c 
LEFT JOIN channel_members cm ON c.id = cm.channel_id AND cm.left_at IS NULL
WHERE c.channel_type = 1
GROUP BY c.id, c.channel_type, c.member_count
ORDER BY c.id;

-- 2. 查看好友关系（从 relationship_db）
-- 需要根据好友关系来确定哪些用户应该在同一个私聊频道

-- 3. 手动修复 channel 3 (假设是用户 5 和 6 的私聊)
-- 先检查 channel 3 的成员
SELECT * FROM channel_members WHERE channel_id = 3;

-- 如果只有用户 5，添加用户 6
INSERT INTO channel_members (channel_id, user_id, role, unread_count, mention_count, pinned, show_nickname, joined_at)
SELECT 3, 6, 1, 0, 0, false, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM channel_members WHERE channel_id = 3 AND user_id = 6);

-- 4. 手动修复 channel 4 (假设是用户 6 和 5 的私聊)
-- 先检查 channel 4 的成员
SELECT * FROM channel_members WHERE channel_id = 4;

-- 如果只有用户 6，添加用户 5
INSERT INTO channel_members (channel_id, user_id, role, unread_count, mention_count, pinned, show_nickname, joined_at)
SELECT 4, 5, 1, 0, 0, false, true, NOW()
WHERE NOT EXISTS (SELECT 1 FROM channel_members WHERE channel_id = 4 AND user_id = 5);

-- 5. 更新所有私聊频道的实际成员数
UPDATE channels c
SET member_count = (
    SELECT COUNT(*) FROM channel_members cm 
    WHERE cm.channel_id = c.id AND cm.left_at IS NULL
)
WHERE c.channel_type = 1;

-- 6. 验证修复结果
SELECT c.id as channel_id, c.member_count, 
       array_agg(cm.user_id ORDER BY cm.user_id) as members
FROM channels c 
JOIN channel_members cm ON c.id = cm.channel_id AND cm.left_at IS NULL
WHERE c.channel_type = 1
GROUP BY c.id, c.member_count
ORDER BY c.id;
