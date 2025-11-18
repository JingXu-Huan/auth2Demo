import api from './index'

/**
 * 群组服务 API
 * 对应后端: IM-relationship-server (8003端口)
 * 路径前缀: /api/v1/groups
 */
export const groupServiceAPI = {
  // ==================== 群组管理 ====================
  
  /**
   * 创建群组
   * @param {Object} request - 创建群组请求
   * @param {string} request.name - 群组名称
   * @param {string} request.description - 群组描述
   * @param {string} request.avatar - 群组头像URL
   * @param {number} request.maxMembers - 最大成员数
   * @param {string} request.joinType - 加入方式: OPEN/APPROVAL/INVITE
   * @param {Array<number>} request.initialMemberIds - 初始成员ID列表
   * @param {number} request.ownerId - 群主ID
   */
  createGroup: (request) => api.post('/v1/groups', request),

  /**
   * 获取群组信息
   * @param {string} groupId - 群组ID
   */
  getGroup: (groupId) => api.get(`/v1/groups/${groupId}`),

  /**
   * 更新群组信息
   * @param {string} groupId - 群组ID
   * @param {Object} request - 更新请求
   * @param {string} request.name - 群组名称（可选）
   * @param {string} request.description - 群组描述（可选）
   * @param {string} request.avatar - 群组头像（可选）
   * @param {number} request.maxMembers - 最大成员数（可选）
   * @param {string} request.joinType - 加入方式（可选）
   */
  updateGroup: (groupId, request) => api.put(`/v1/groups/${groupId}`, request),

  /**
   * 解散群组
   * @param {string} groupId - 群组ID
   */
  dissolveGroup: (groupId) => api.delete(`/v1/groups/${groupId}`),

  // ==================== 成员管理 ====================
  
  /**
   * 添加群成员
   * @param {string} groupId - 群组ID
   * @param {Object} request - 添加成员请求
   * @param {Array<number>} request.userIds - 用户ID列表
   * @param {number} request.inviterId - 邀请人ID
   */
  addMembers: (groupId, request) => api.post(`/v1/groups/${groupId}/members`, request),

  /**
   * 移除群成员
   * @param {string} groupId - 群组ID
   * @param {number} userId - 用户ID
   */
  removeMember: (groupId, userId) => api.delete(`/v1/groups/${groupId}/members/${userId}`),

  /**
   * 获取群成员列表
   * @param {string} groupId - 群组ID
   * @param {number} page - 页码，默认1
   * @param {number} size - 每页大小，默认20
   * @param {string} role - 角色过滤: ALL/OWNER/ADMIN/MEMBER
   */
  getMembers: (groupId, page = 1, size = 20, role = 'ALL') =>
    api.get(`/v1/groups/${groupId}/members`, {
      params: { page, size, role }
    }),

  /**
   * 搜索群成员
   * @param {string} groupId - 群组ID
   * @param {string} keyword - 搜索关键字
   * @param {number} limit - 返回数量限制，默认20
   */
  searchMembers: (groupId, keyword, limit = 20) =>
    api.get(`/v1/groups/${groupId}/members/search`, {
      params: { keyword, limit }
    }),

  /**
   * 用户退出群组
   * @param {string} groupId - 群组ID
   * @param {number} userId - 用户ID
   */
  leaveGroup: (groupId, userId) =>
    api.delete(`/v1/groups/${groupId}/members/leave`, {
      params: { userId }
    }),

  // ==================== 角色管理 ====================
  
  /**
   * 设置/取消管理员
   * @param {string} groupId - 群组ID
   * @param {Object} request - 设置管理员请求
   * @param {number} request.userId - 用户ID
   * @param {string} request.action - 操作: ADD/REMOVE
   */
  setAdmin: (groupId, request) => api.post(`/v1/groups/${groupId}/admins`, request),

  /**
   * 转让群主
   * @param {string} groupId - 群组ID
   * @param {number} newOwnerId - 新群主ID
   */
  transferOwnership: (groupId, newOwnerId) =>
    api.post(`/v1/groups/${groupId}/transfer`, null, {
      params: { newOwnerId }
    }),

  // ==================== 群组查询 ====================
  
  /**
   * 获取用户加入的群组列表
   * @param {number} userId - 用户ID
   * @param {number} page - 页码，默认1
   * @param {number} size - 每页大小，默认20
   */
  getUserGroups: (userId, page = 1, size = 20) =>
    api.get(`/v1/groups/user/${userId}`, {
      params: { page, size }
    }),

  /**
   * 搜索群组
   * @param {string} keyword - 搜索关键字
   * @param {number} page - 页码
   * @param {number} size - 每页大小
   */
  searchGroups: (keyword, page = 1, size = 20) =>
    api.get('/v1/groups/search', {
      params: { keyword, page, size }
    }),

  // ==================== 群组设置 ====================
  
  /**
   * 设置群成员禁言
   * @param {string} groupId - 群组ID
   * @param {number} userId - 用户ID
   * @param {boolean} muted - 是否禁言
   */
  muteMember: (groupId, userId, muted) =>
    api.put(`/v1/groups/${groupId}/members/${userId}/mute`, null, {
      params: { muted }
    }),

  /**
   * 设置全员禁言
   * @param {string} groupId - 群组ID
   * @param {boolean} muteAll - 是否全员禁言
   */
  muteAll: (groupId, muteAll) =>
    api.put(`/v1/groups/${groupId}/mute-all`, null, {
      params: { muteAll }
    }),

  /**
   * 设置群昵称
   * @param {string} groupId - 群组ID
   * @param {number} userId - 用户ID
   * @param {string} nickname - 群昵称
   */
  setGroupNickname: (groupId, userId, nickname) =>
    api.put(`/v1/groups/${groupId}/members/${userId}/nickname`, null, {
      params: { nickname }
    })
}

/**
 * 好友服务 API
 * 对应后端: IM-relationship-server (8003端口)
 * 路径前缀: /api/v1/friends
 */
export const friendServiceAPI = {
  // ==================== 好友请求 ====================
  
  /**
   * 发送好友请求
   * @param {Object} request - 好友请求
   * @param {number} request.fromUserId - 发起人ID
   * @param {number} request.toUserId - 目标用户ID
   * @param {string} request.message - 验证消息
   */
  sendFriendRequest: (request) => api.post('/v1/friends/request', request),

  /**
   * 接受好友请求
   * @param {number} fromUserId - 发起人ID
   * @param {number} toUserId - 目标用户ID
   */
  acceptFriendRequest: (fromUserId, toUserId) =>
    api.post('/v1/friends/request/accept', null, {
      params: { fromUserId, toUserId }
    }),

  /**
   * 拒绝好友请求
   * @param {number} fromUserId - 发起人ID
   * @param {number} toUserId - 目标用户ID
   */
  rejectFriendRequest: (fromUserId, toUserId) =>
    api.post('/v1/friends/request/reject', null, {
      params: { fromUserId, toUserId }
    }),

  /**
   * 获取收到的好友请求列表
   * @param {number} userId - 用户ID
   */
  getReceivedRequests: (userId) =>
    api.get(`/v1/friends/requests/received/${userId}`),

  /**
   * 获取发出的好友请求列表
   * @param {number} userId - 用户ID
   */
  getSentRequests: (userId) =>
    api.get(`/v1/friends/requests/sent/${userId}`),

  // ==================== 好友管理 ====================
  
  /**
   * 获取好友列表
   * @param {number} userId - 用户ID
   */
  getFriends: (userId) => api.get(`/v1/friends/${userId}`),

  /**
   * 删除好友
   * @param {number} userId - 用户ID
   * @param {number} friendId - 好友ID
   */
  deleteFriend: (userId, friendId) =>
    api.delete(`/v1/friends/${userId}/friends/${friendId}`),

  /**
   * 搜索好友
   * @param {number} userId - 用户ID
   * @param {string} keyword - 搜索关键字
   * @param {number} limit - 返回数量限制，默认20
   */
  searchFriends: (userId, keyword, limit = 20) =>
    api.get(`/v1/friends/${userId}/search`, {
      params: { keyword, limit }
    }),

  /**
   * 设置好友备注
   * @param {number} userId - 用户ID
   * @param {number} friendId - 好友ID
   * @param {string} remark - 备注名称
   */
  setFriendRemark: (userId, friendId, remark) =>
    api.put(`/v1/friends/${userId}/friends/${friendId}/remark`, null, {
      params: { remark }
    }),

  /**
   * 拉黑/取消拉黑好友
   * @param {number} userId - 用户ID
   * @param {number} friendId - 好友ID
   * @param {boolean} blocked - 是否拉黑
   */
  blockFriend: (userId, friendId, blocked) =>
    api.put(`/v1/friends/${userId}/friends/${friendId}/block`, null, {
      params: { blocked }
    })
}

export default {
  group: groupServiceAPI,
  friend: friendServiceAPI
}
