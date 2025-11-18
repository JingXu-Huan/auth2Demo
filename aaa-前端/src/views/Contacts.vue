<template>
  <div class="contacts-container">
    <a-layout class="contacts-layout">
      <!-- 左侧导航 -->
      <a-layout-sider width="240" class="contacts-sider">
        <div class="sider-header">
          <h3>通讯录</h3>
        </div>
        
        <!-- 左侧搜索框已删除，避免冗余 -->
        
        <a-menu 
          v-model:selectedKeys="selectedKeys"
          mode="inline"
          class="contact-menu"
          @select="handleMenuSelect"
        >
          <a-menu-item key="friends">
            <template #icon>
              <user-outlined />
            </template>
            我的好友
            <span class="menu-count" v-if="friends.length > 0">{{ friends.length }}</span>
          </a-menu-item>
          <a-menu-item key="organization">
            <template #icon>
              <apartment-outlined />
            </template>
            组织架构
          </a-menu-item>
          <a-menu-item key="groups">
            <template #icon>
              <team-outlined />
            </template>
            我的群组
            <span class="menu-count" v-if="groups.length > 0">{{ groups.length }}</span>
          </a-menu-item>
          <a-menu-item key="requests">
            <template #icon>
              <message-outlined />
            </template>
            好友申请
            <a-badge :count="friendRequests.length" :offset="[10, 0]" v-if="friendRequests.length > 0" />
          </a-menu-item>
        </a-menu>
      </a-layout-sider>
    
      <!-- 右侧内容区域 -->
      <a-layout-content class="contacts-content">
        <!-- 好友列表 -->
        <div v-if="activeCategory === 'friends'" class="friends-section">
          <!-- 顶部工具栏 -->
          <div class="section-header">
            <div class="header-left">
              <h2>我的好友</h2>
              <span class="count-text">({{ friends.length }})</span>
            </div>
            <div class="header-right">
              <a-input 
                v-model:value="friendListFilterKey" 
                placeholder="搜索好友..."
                allow-clear
                style="width: 240px; margin-right: 12px;"
              >
                <template #prefix>
                  <search-outlined />
                </template>
              </a-input>
              <a-button type="primary" @click="showAddFriendDialog = true">
                <template #icon>
                  <plus-outlined />
                </template>
                添加好友
              </a-button>
            </div>
          </div>
          
          <div v-if="filteredFriends.length === 0" class="empty-container">
            <p class="empty-text">暂无好友</p>
          </div>
          
          <!-- 列表视图 -->
          <div v-else class="friend-list">
            <div 
              v-for="friend in filteredFriends" 
              :key="friend.userId"
              class="friend-row"
              @click="viewContact(friend)"
            >
              <a-avatar :src="friend.avatar" :size="40" class="row-avatar">
                <template #icon>
                  <user-outlined />
                </template>
                {{ friend.nickname?.charAt(0) || 'U' }}
              </a-avatar>
              
              <div class="row-info">
                <div class="row-name">
                  {{ friend.nickname }}
                  <a-tag 
                    v-if="friend.status === 'online'" 
                    color="success" 
                    size="small"
                    class="row-badge"
                  >
                    在线
                  </a-tag>
                </div>
                <div class="row-desc">{{ friend.signature || '暂无个性签名' }}</div>
              </div>

              <div class="row-actions">
                <a-tooltip title="发送消息">
                  <a-button 
                    class="icon-btn" 
                    type="text"
                    @click.stop="startChat(friend)"
                  >
                    <template #icon>
                      <message-outlined />
                    </template>
                  </a-button>
                </a-tooltip>
                <a-tooltip title="语音通话">
                  <a-button 
                    class="icon-btn" 
                    type="text"
                    @click.stop="makeCall(friend)"
                  >
                    <template #icon>
                      <phone-outlined />
                    </template>
                  </a-button>
                </a-tooltip>
              </div>
            </div>
          </div>
        </div>
      
        <!-- 组织架构 -->
        <div v-else-if="activeCategory === 'organization'" class="organization-section">
          <!-- 有组织时：显示架构树 -->
          <div v-if="hasOrganization" class="org-content">
            <div class="section-header">
              <div class="header-left">
                <h2>{{ currentOrganization?.name || '组织架构' }}</h2>
                <span class="count-text">共 {{ organizationMembers.length }} 人</span>
              </div>
              <div class="header-actions">
                <a-button @click="refreshOrganization">
                  <template #icon>
                    <reload-outlined />
                  </template>
                  刷新
                </a-button>
                <a-button type="primary" @click="showInviteMemberDialog = true">
                  <template #icon>
                    <plus-outlined />
                  </template>
                  邀请成员
                </a-button>
              </div>
            </div>
            
            <!-- 组织成员列表 -->
            <div class="contacts-grid">
              <a-card 
                v-for="member in organizationMembers" 
                :key="member.id"
                class="contact-card"
                :hoverable="true"
                @click="viewColleagueDetail(member)"
              >
                <div class="card-content">
                  <a-avatar :src="member.avatar" :size="72" class="contact-avatar">
                    <template #icon>
                      <user-outlined />
                    </template>
                    {{ member.name?.charAt(0) || 'U' }}
                  </a-avatar>
                  <div class="contact-info">
                    <h4 class="contact-name">{{ member.name }}</h4>
                    <p class="contact-dept">
                      <span v-if="member.department" class="dept-tag">{{ member.department }}</span>
                      <span class="title-text">{{ member.position || '员工' }}</span>
                    </p>
                  </div>
                  <div class="contact-actions">
                    <a-tooltip title="发送消息">
                      <a-button 
                        type="primary" 
                        shape="circle" 
                        size="small"
                        @click.stop="startChatWithColleague(member)"
                      >
                        <template #icon>
                          <message-outlined />
                        </template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip title="查看详情">
                      <a-button 
                        shape="circle" 
                        size="small"
                        @click.stop="viewColleagueDetail(member)"
                      >
                        <template #icon>
                          <folder-outlined />
                        </template>
                      </a-button>
                    </a-tooltip>
                  </div>
                </div>
              </a-card>
            </div>
          </div>

          <!-- 无组织时：新手引导 -->
          <div v-else class="org-onboarding-container">
            <!-- 创建组织卡片 -->
            <div class="action-card primary-card" @click="showCreateOrgDialog = true">
              <div class="icon-large">
                <plus-outlined />
              </div>
              <h3 class="card-title">创建组织</h3>
              <p class="card-description">新建一个团队，邀请同事加入，开始协作</p>
              <a-button type="primary" size="large" shape="round">
                立即创建
              </a-button>
            </div>

            <!-- 分隔符 -->
            <div class="card-divider"></div>

            <!-- 加入组织卡片 -->
            <div class="action-card secondary-card" @click="showJoinOrgDialog = true">
              <div class="icon-large">
                <team-outlined />
              </div>
              <h3 class="card-title">加入组织</h3>
              <p class="card-description">输入团队邀请码，或搜索团队名称加入</p>
              <a-button size="large" shape="round" class="secondary-button">
                加入团队
              </a-button>
            </div>
          </div>
        </div>
      
        <!-- 群组列表 -->
        <div v-else-if="activeCategory === 'groups'" class="groups-section">
          <div class="section-header">
            <div class="header-left">
              <h2>我的群组</h2>
              <span class="count-text">共 {{ groups.length }} 个群组</span>
            </div>
            <a-button type="primary" @click="createGroup">
              <template #icon>
                <plus-outlined />
              </template>
              创建群组
            </a-button>
          </div>
          
          <!-- 加载中骨架屏 -->
          <div v-if="loadingGroups" class="groups-skeleton">
            <a-skeleton active :paragraph="{ rows: 4 }" />
          </div>
          
          <!-- 空状态 -->
          <div v-else-if="groups.length === 0" class="empty-container">
            <div ref="groupsEmptyLottieContainer" class="empty-lottie"></div>
          </div>
          
          <!-- 群组列表 -->
          <div v-else class="groups-list">
            <a-card 
              v-for="group in groups" 
              :key="group.id"
              class="group-card"
              :hoverable="true"
              @click="viewGroup(group)"
            >
              <div class="group-item">
                <a-avatar :src="group.avatar" :size="56" shape="square" class="group-avatar">
                  <template #icon>
                    <team-outlined />
                  </template>
                  {{ group.name?.charAt(0) || 'G' }}
                </a-avatar>
                <div class="group-info">
                  <div class="group-header-row">
                    <h4 class="group-name">{{ group.name }}</h4>
                    <span class="member-count-badge">{{ group.memberCount }} 人</span>
                  </div>
                  <p class="group-desc">{{ group.description || '暂无描述' }}</p>
                  <div class="group-meta">
                    <span class="meta-item">
                      <clock-circle-outlined />
                      {{ formatTime(group.lastActiveTime) }}
                    </span>
                  </div>
                </div>
                <div class="group-actions">
                  <a-button type="primary" size="small" @click.stop="enterGroup(group)">
                    <template #icon>
                      <message-outlined />
                    </template>
                    进入群聊
                  </a-button>
                </div>
              </div>
            </a-card>
          </div>
        </div>
      
        <!-- 好友申请 -->
        <div v-else-if="activeCategory === 'requests'" class="requests-section">
          <div class="section-header">
            <div class="header-left">
              <h2>好友申请</h2>
              <span class="count-text">{{ friendRequests.length }} 条申请</span>
            </div>
            <a-button shape="circle" @click="loadFriendRequests">
              <template #icon>
                <reload-outlined />
              </template>
            </a-button>
          </div>
          
          <div v-if="friendRequests.length === 0" class="empty-container">
            <div ref="requestsEmptyLottieContainer" class="empty-lottie"></div>
            <p class="empty-text">暂无好友申请</p>
          </div>
          
          <div v-else class="requests-list">
            <a-card 
              v-for="request in friendRequests" 
              :key="request.requestId"
              class="request-card"
            >
              <div class="request-item">
                <a-avatar :src="request.fromAvatar" :size="48" class="request-avatar">
                  <template #icon>
                    <user-outlined />
                  </template>
                  {{ request.fromNickname?.charAt(0) || 'U' }}
                </a-avatar>
                <div class="request-info">
                  <h4 class="request-name">{{ request.fromNickname || '未知用户' }}</h4>
                  <p class="request-message">{{ request.message || '请求添加您为好友' }}</p>
                  <span class="request-time">
                    <clock-circle-outlined />
                    {{ formatTime(request.createdAt) }}
                  </span>
                </div>
                <div class="request-actions">
                  <a-button 
                    type="primary" 
                    size="small" 
                    @click="handleFriendRequest(request, 'accept')"
                  >
                    同意
                  </a-button>
                  <a-button 
                    size="small" 
                    @click="handleFriendRequest(request, 'reject')"
                  >
                    拒绝
                  </a-button>
                </div>
              </div>
            </a-card>
          </div>
        </div>
      </a-layout-content>
    </a-layout>
    
    <!-- 添加好友对话框 - 飞书风格 -->
    <a-modal
      v-model:open="showAddFriendDialog"
      title="添加好友"
      width="520px"
      :footer="null"
      class="add-friend-modal"
    >
      <a-tabs v-model:activeKey="addFriendTab" class="add-friend-tabs">
        <!-- Tab 1: 搜索添加 -->
        <a-tab-pane key="search" tab="搜索添加">
          <div class="tab-content">
            <!-- 搜索框 -->
            <div class="search-box">
              <a-input
                v-model:value="addFriendSearchKey"
                placeholder="请输入邮箱 / 手机号 / Lantis号"
                size="large"
                @pressEnter="handleSearchUser"
              >
                <template #suffix>
                  <a-button
                    type="text"
                    :loading="searchLoading"
                    @click="handleSearchUser"
                  >
                    <template #icon>
                      <search-outlined />
                    </template>
                  </a-button>
                </template>
              </a-input>
            </div>

            <!-- 搜索结果 -->
            <div v-if="searchResult" class="user-card">
              <a-avatar :src="searchResult.avatar" :size="48">
                {{ searchResult.nickname?.charAt(0) || 'U' }}
              </a-avatar>
              <div class="user-info">
                <div class="user-name">{{ searchResult.nickname }}</div>
                <div class="user-desc">{{ searchResult.email || searchResult.phone || 'Lantis用户' }}</div>
              </div>
              <a-button
                type="primary"
                size="small"
                @click="showVerifyInput = true"
              >
                <template #icon>
                  <plus-outlined />
                </template>
                添加好友
              </a-button>
            </div>

            <!-- 验证消息输入 -->
            <div v-if="showVerifyInput" class="verify-area">
              <div class="verify-title">验证消息</div>
              <a-textarea
                v-model:value="verifyMessage"
                placeholder="请输入验证消息（可选）"
                :rows="3"
                :maxlength="100"
              />
              <div class="verify-actions">
                <a-button @click="showVerifyInput = false">取消</a-button>
                <a-button
                  type="primary"
                  :loading="sendingRequest"
                  @click="handleSendRequest"
                >
                  发送请求
                </a-button>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="!searchResult && !searchLoading" class="empty-hint">
              <user-outlined class="empty-icon" />
              <p>输入邮箱、手机号或Lantis号搜索好友</p>
            </div>
          </div>
        </a-tab-pane>

        <!-- Tab 2: 链接邀请 -->
        <a-tab-pane key="link" tab="链接邀请">
          <div class="tab-content invite-content">
            <!-- 二维码 -->
            <div class="qr-section">
              <div class="qr-code">
                <qrcode-outlined class="qr-placeholder" />
                <div class="qr-text">二维码</div>
              </div>
              <p class="qr-tip">扫描二维码添加我为好友</p>
            </div>

            <!-- 邀请链接 -->
            <div class="link-section">
              <div class="link-title">分享链接给好友</div>
              <div class="link-row">
                <a-input
                  :value="inviteLink"
                  readonly
                  class="link-input"
                />
                <a-button type="primary" @click="handleCopyLink">
                  <template #icon>
                    <copy-outlined />
                  </template>
                  复制链接
                </a-button>
              </div>
              <p class="link-tip">链接有效期7天，对方可通过链接直接添加你为好友</p>
            </div>

            <!-- 重置链接 -->
            <div class="reset-section">
              <a-button type="link" @click="handleResetLink">
                <template #icon>
                  <reload-outlined />
                </template>
                重置链接
              </a-button>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
    
    <!-- 创建群组对话框 - 飞书风格 -->
    <a-modal
      v-model:open="showCreateGroupDialog"
      title="创建群组"
      width="760px"
      :footer="null"
      class="create-group-modal"
    >
      <div class="create-group-layout">
        <!-- 左侧：群信息 -->
        <div class="group-info-section">
          <!-- 群头像 -->
          <div class="avatar-upload-section">
            <div class="avatar-uploader-modern" @click="handleAvatarClick">
              <div v-if="!createGroupForm.avatar" class="avatar-placeholder-modern">
                <camera-outlined class="camera-icon-modern" />
                <div class="avatar-text-modern">{{ getAvatarText(createGroupForm.name) }}</div>
              </div>
              <img v-else :src="createGroupForm.avatar" class="avatar-preview-modern" />
              <div class="avatar-hover-mask-modern">
                <camera-outlined />
                <span>更换头像</span>
              </div>
            </div>
            <input
              ref="avatarInput"
              type="file"
              accept="image/*"
              style="display: none"
              @change="handleAvatarChange"
            />
          </div>

          <!-- 群信息表单 -->
          <div class="group-form">
            <div class="form-item required">
              <label>群名称</label>
              <a-input
                v-model:value="createGroupForm.name"
                placeholder="例如：产品研发沟通群"
                :maxlength="30"
                @input="updateAvatarPreview"
              />
            </div>
            <!-- 群简介 -->
          <div class="form-item">
            <label>群简介</label>
            <a-textarea
              v-model:value="createGroupForm.description"
              placeholder="可选，介绍一下这个群组"
              :rows="3"
              :maxlength="200"
            />
          </div>

            <div class="form-item">
              <label>入群方式</label>
              <a-select
                v-model:value="createGroupForm.joinType"
                style="width: 100%"
              >
                <a-select-option value="FREE">直接加入</a-select-option>
                <a-select-option value="APPROVAL">需要审批</a-select-option>
                <a-select-option value="INVITE">仅邀请</a-select-option>
              </a-select>
            </div>
          </div>
        </div>

        <!-- 分割线 -->
        <div class="divider-vertical"></div>

        <!-- 右侧：成员选择 -->
        <div class="member-select-section">
          <div class="section-title">
            <team-outlined />
            选择成员
            <span class="member-count">已选 {{ selectedMembers.size }} 人</span>
          </div>

          <!-- 搜索框 -->
          <a-input
            v-model:value="memberSearchKeyword"
            placeholder="搜索人员..."
            allow-clear
            class="member-search"
          >
            <template #prefix>
              <search-outlined />
            </template>
          </a-input>

          <!-- Tab 切换 -->
          <a-tabs v-model:activeKey="memberSourceTab" class="member-tabs">
            <!-- Tab 1: 我的好友 -->
            <a-tab-pane key="friends" tab="我的好友">
              <div class="member-list">
                <div
                  v-for="friend in filteredFriendsForCreate"
                  :key="friend.userId"
                  class="member-list-item"
                  @click="toggleCreateMember(friend)"
                >
                  <a-checkbox :checked="selectedMembers.has(friend.userId)" />
                  <a-avatar :src="friend.avatar" :size="36">
                    {{ friend.nickname?.charAt(0) || 'U' }}
                  </a-avatar>
                  <div class="info-column">
                    <div class="name-text">{{ friend.nickname }}</div>
                    <div v-if="friend.department || friend.email" class="sub-text">
                      {{ friend.department || friend.email }}
                    </div>
                  </div>
                </div>
              </div>
            </a-tab-pane>

            <!-- Tab 2: 组织架构 -->
            <a-tab-pane key="org" tab="组织架构">
              <div class="org-tree-container">
                <a-tree
                  v-if="organizationMembers.length > 0"
                  :tree-data="orgTreeData"
                  checkable
                  :checked-keys="orgCheckedKeys"
                  @check="handleOrgCheck"
                >
                  <template #title="{ title, avatar }">
                    <div class="tree-node-title">
                      <a-avatar v-if="avatar" :src="avatar" :size="24">
                        {{ title?.charAt(0) || 'U' }}
                      </a-avatar>
                      <span>{{ title }}</span>
                    </div>
                  </template>
                </a-tree>
                <a-empty v-else description="暂无组织架构" />
              </div>
            </a-tab-pane>

            <!-- Tab 3: 从群组导入 -->
            <a-tab-pane key="groups" tab="从群组导入">
              <!-- 群组列表 -->
              <div v-if="!selectedSourceGroup" class="group-list">
                <div
                  v-for="group in groups"
                  :key="group.id"
                  class="group-list-item"
                  @click="enterSourceGroup(group)"
                >
                  <a-avatar :src="group.avatar" :size="40" shape="square">
                    {{ group.name?.charAt(0) || 'G' }}
                  </a-avatar>
                  <div class="group-info">
                    <div class="group-name">{{ group.name }}</div>
                    <div class="group-count">{{ group.memberCount }} 人</div>
                  </div>
                  <right-outlined class="arrow-icon" />
                </div>
              </div>

              <!-- 群成员列表 -->
              <div v-else class="group-members">
                <div class="back-header" @click="selectedSourceGroup = null">
                  <left-outlined />
                  返回群组列表
                </div>
                <div class="member-list">
                  <div
                    v-for="member in sourceGroupMembers"
                    :key="member.userId"
                    class="member-list-item"
                    @click="toggleCreateMember(member)"
                  >
                    <a-checkbox :checked="selectedMembers.has(member.userId)" />
                    <a-avatar :src="member.avatar" :size="36">
                      {{ member.nickname?.charAt(0) || 'U' }}
                    </a-avatar>
                    <div class="info-column">
                      <div class="name-text">{{ member.nickname }}</div>
                      <div v-if="member.department || member.email" class="sub-text">
                        {{ member.department || member.email }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>

          <!-- 已选成员（横向滚动） -->
          <div v-if="selectedMembers.size > 0" class="selected-footer-pro">
            <div class="footer-title">已选成员 ({{ selectedMembers.size }})</div>
            <div class="avatar-row">
              <div
                v-for="member in Array.from(selectedMembers.values())"
                :key="member.userId"
                class="avatar-wrapper"
                @click="removeMember(member.userId)"
              >
                <a-avatar :src="member.avatar" :size="32">
                  {{ member.nickname?.charAt(0) || 'U' }}
                </a-avatar>
                <span class="close-badge">×</span>
                <div class="member-tooltip">{{ member.nickname }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="modal-footer">
        <a-button @click="showCreateGroupDialog = false">取消</a-button>
        <a-button
          type="primary"
          :disabled="!createGroupForm.name || createGroupForm.name.length < 2"
          :loading="createGroupLoading"
          @click="handleCreateGroup"
        >
          立即创建
        </a-button>
      </div>
    </a-modal>
    

    <!-- 同事详情抽屉 -->
    <a-drawer
      v-model:open="showColleagueDrawer"
      title="同事详情"
      placement="right"
      :width="400"
    >
      <div v-if="selectedColleague" class="colleague-detail">
        <div class="detail-header">
          <a-avatar :src="selectedColleague.avatar" :size="80">
            <template #icon>
              <user-outlined />
            </template>
            {{ selectedColleague.name?.charAt(0) || 'U' }}
          </a-avatar>
          <h3>{{ selectedColleague.name }}</h3>
          <p class="dept-position">
            <span class="dept-tag-large">{{ selectedColleague.department }}</span>
            <span class="position-text">{{ selectedColleague.position }}</span>
          </p>
        </div>

        <a-divider />

        <div class="detail-info">
          <div class="info-item" v-if="selectedColleague.email">
            <span class="label">邮箱</span>
            <span class="value">{{ selectedColleague.email }}</span>
          </div>
          <div class="info-item" v-if="selectedColleague.phone">
            <span class="label">手机</span>
            <span class="value">{{ selectedColleague.phone }}</span>
          </div>
          <div class="info-item" v-if="selectedColleague.employeeId">
            <span class="label">工号</span>
            <span class="value">{{ selectedColleague.employeeId }}</span>
          </div>
        </div>

        <a-divider />

        <div class="detail-actions">
          <a-button type="primary" block @click="startChatWithColleague(selectedColleague)">
            <template #icon>
              <message-outlined />
            </template>
            发送消息
          </a-button>
          <a-button block style="margin-top: 12px;">
            <template #icon>
              <phone-outlined />
            </template>
            语音通话
          </a-button>
        </div>
      </div>
    </a-drawer>

    <!-- 创建组织对话框 - 专业版 -->
    <a-modal
      v-model:open="showCreateOrgDialog"
      width="580px"
      class="create-org-dialog"
    >
      <template #title>
        <div class="dialog-title">创建组织</div>
      </template>

      <div class="dialog-content">
        <!-- Logo上传区域 - 顶部居中 -->
        <div class="org-logo-uploader">
          <div class="logo-upload-area" @click="handleOrgLogoClick">
            <div v-if="!createOrgForm.logo" class="logo-placeholder-pro">
              <camera-outlined class="camera-icon-pro" />
              <div class="logo-text-pro">{{ getOrgLogoText(createOrgForm.name) }}</div>
            </div>
            <img v-else :src="createOrgForm.logo" class="logo-preview-pro" />
            <div class="logo-hover-overlay">
              <camera-outlined />
              <span>上传Logo</span>
            </div>
          </div>
          <input
            ref="orgLogoInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleOrgLogoChange"
          />
        </div>

        <!-- 表单区域 -->
        <div class="org-form-pro">
          <!-- 组织名称 -->
          <div class="form-item-pro required">
            <label class="form-label">组织名称</label>
            <a-input
              v-model:value="createOrgForm.name"
              placeholder="请输入组织全称"
              size="large"
              :maxlength="50"
              @input="updateOrgLogoText"
            />
          </div>

          <!-- 行业和规模 - 两列布局 -->
          <div class="form-row">
            <div class="form-item-pro form-col">
              <label class="form-label">所属行业</label>
              <a-select
                v-model:value="createOrgForm.industry"
                placeholder="请选择所属行业"
                size="large"
                style="width: 100%"
              >
                <a-select-option value="互联网">互联网</a-select-option>
                <a-select-option value="教育">教育</a-select-option>
                <a-select-option value="金融">金融</a-select-option>
                <a-select-option value="制造业">制造业</a-select-option>
                <a-select-option value="医疗健康">医疗健康</a-select-option>
                <a-select-option value="其他">其他</a-select-option>
              </a-select>
            </div>

            <div class="form-item-pro form-col">
              <label class="form-label">组织规模</label>
              <a-select
                v-model:value="createOrgForm.scale"
                placeholder="请选择组织规模"
                size="large"
                style="width: 100%"
              >
                <a-select-option value="1-50">1-50人</a-select-option>
                <a-select-option value="51-200">51-200人</a-select-option>
                <a-select-option value="201-500">201-500人</a-select-option>
                <a-select-option value="500+">500人以上</a-select-option>
              </a-select>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部按钮 - 仅保留主操作 -->
      <template #footer>
        <div class="dialog-footer-single">
          <a-button
            type="primary"
            size="large"
            :disabled="!createOrgForm.name || createOrgForm.name.length < 2"
            :loading="creatingOrg"
            @click="handleCreateOrganization"
          >
            创建
          </a-button>
        </div>
      </template>
    </a-modal>

    <!-- 加入组织对话框 -->
    <a-modal
      v-model:open="showJoinOrgDialog"
      title="加入组织"
      width="560px"
      :footer="null"
      :body-style="{ minHeight: '400px' }"
      class="join-org-dialog"
    >
      <a-tabs v-model:activeKey="joinOrgTab">
        <!-- Tab 1: 搜索加入 -->
        <a-tab-pane key="search" tab="搜索加入">
          <div class="join-org-content">
            <div class="search-input-group">
              <a-input
                v-model:value="orgSearchKeyword"
                placeholder="请输入组织ID / 邀请码"
                size="large"
                @pressEnter="handleSearchOrg"
              />
              <a-button
                type="primary"
                size="large"
                :loading="searchingOrg"
                @click="handleSearchOrg"
              >
                <template #icon>
                  <search-outlined />
                </template>
                搜索
              </a-button>
            </div>

            <!-- 搜索结果 -->
            <div v-if="orgSearchResult" class="org-result-card">
              <a-avatar :src="orgSearchResult.logo" :size="56" shape="square">
                {{ orgSearchResult.name?.charAt(0) || '企' }}
              </a-avatar>
              <div class="org-info">
                <div class="org-name">{{ orgSearchResult.name }}</div>
                <div class="org-desc">{{ orgSearchResult.industry }} · {{ orgSearchResult.scale }}</div>
              </div>
              <a-button type="primary" @click="handleApplyJoin">
                申请加入
              </a-button>
            </div>
          </div>
        </a-tab-pane>

        <!-- Tab 2: 待处理邀请 -->
        <a-tab-pane key="pending" tab="待处理邀请">
          <div class="pending-invites">
            <a-empty v-if="pendingInvites.length === 0" description="暂无待处理邀请" />
            <div v-else>
              <div
                v-for="invite in pendingInvites"
                :key="invite.id"
                class="invite-item"
              >
                <a-avatar :src="invite.orgLogo" :size="48" shape="square">
                  {{ invite.orgName?.charAt(0) || '企' }}
                </a-avatar>
                <div class="invite-info">
                  <div class="invite-text">
                    <strong>{{ invite.inviterName }}</strong> 邀请你加入
                    <strong>{{ invite.orgName }}</strong>
                  </div>
                  <div class="invite-time">{{ formatTime(invite.createdAt) }}</div>
                </div>
                <div class="invite-actions">
                  <a-button type="primary" size="small" @click="handleAcceptInvite(invite)">
                    接受
                  </a-button>
                  <a-button size="small" @click="handleRejectInvite(invite)">
                    拒绝
                  </a-button>
                </div>
              </div>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <!-- 邀请成员对话框 -->
    <a-modal
      v-model:open="showInviteMemberDialog"
      title="邀请成员"
      width="560px"
      :footer="null"
    >
      <a-tabs v-model:activeKey="inviteMemberTab">
        <!-- Tab 1: 链接/二维码邀请 -->
        <a-tab-pane key="link" tab="链接邀请">
          <div class="invite-link-content">
            <!-- 二维码 -->
            <div class="qr-section">
              <div class="qr-code">
                <qrcode-outlined class="qr-placeholder" />
                <div class="qr-text">二维码</div>
              </div>
              <p class="qr-tip">扫描二维码加入组织</p>
            </div>

            <!-- 邀请链接 -->
            <div class="link-section">
              <div class="link-title">发送链接给同事，点击即可加入</div>
              <div class="link-row">
                <a-input
                  :value="orgInviteLink"
                  readonly
                  class="link-input"
                />
                <a-button type="primary" @click="handleCopyOrgLink">
                  <template #icon>
                    <copy-outlined />
                  </template>
                  复制链接
                </a-button>
              </div>
              <p class="link-tip">链接7天内有效</p>
            </div>
          </div>
        </a-tab-pane>

        <!-- Tab 2: 添加成员 -->
        <a-tab-pane key="add" tab="添加成员">
          <div class="add-member-content">
            <div class="form-item">
              <label>姓名</label>
              <a-input
                v-model:value="addMemberForm.name"
                placeholder="请输入姓名"
              />
            </div>
            <div class="form-item">
              <label>手机号/邮箱</label>
              <a-input
                v-model:value="addMemberForm.contact"
                placeholder="请输入手机号或邮箱"
              />
            </div>
            <div class="form-actions">
              <a-button
                type="primary"
                :disabled="!addMemberForm.name || !addMemberForm.contact"
                @click="handleAddMember"
              >
                发送邀请
              </a-button>
            </div>
          </div>
        </a-tab-pane>

        <!-- Tab 3: 从好友导入 -->
        <a-tab-pane key="import" tab="从好友导入">
          <div class="import-friends-content">
            <div class="friends-list">
              <div
                v-for="friend in friends"
                :key="friend.userId"
                class="friend-item"
                @click="toggleInviteFriend(friend)"
              >
                <a-checkbox :checked="invitedFriends.has(friend.userId)" />
                <a-avatar :src="friend.avatar" :size="32">
                  {{ friend.nickname?.charAt(0) || 'U' }}
                </a-avatar>
                <span class="friend-name">{{ friend.nickname }}</span>
              </div>
            </div>
            <div class="import-actions">
              <a-button
                type="primary"
                :disabled="invitedFriends.size === 0"
                @click="handleInviteFriends"
              >
                邀请选中的 {{ invitedFriends.size }} 位好友
              </a-button>
            </div>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message as AMessage } from 'ant-design-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  SearchOutlined,
  UserOutlined,
  ApartmentOutlined,
  TeamOutlined,
  MessageOutlined,
  PlusOutlined,
  PhoneOutlined,
  ReloadOutlined,
  ClockCircleOutlined,
  FolderOutlined,
  CameraOutlined,
  QrcodeOutlined,
  CopyOutlined,
  RightOutlined,
  LeftOutlined,
  CloseOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '../stores/user'
import { userAPI, organizationAPI, groupAPI, friendAPI } from '../api'
import dayjs from 'dayjs'
import lottie from 'lottie-web'
import emptyAnimationData from '../asserts/让我看看.json'

const router = useRouter()
const userStore = useUserStore()

// 好友列表过滤搜索（本地过滤）
const friendListFilterKey = ref('')
const activeCategory = ref('friends')
const selectedKeys = ref(['friends'])
const showAddFriendDialog = ref(false)
const showCreateGroupDialog = ref(false)
const createGroupLoading = ref(false)
const searchResults = ref([])

// Lottie 动画相关
const orgEmptyLottieContainer = ref(null)
const groupsEmptyLottieContainer = ref(null)
const requestsEmptyLottieContainer = ref(null)
let orgLottieInstance = null
let groupsLottieInstance = null
let requestsLottieInstance = null

const addFriendForm = reactive({
  keyword: '',
  message: '您好，我想添加您为好友'
})

// 添加好友相关
const addFriendTab = ref('search')
const addFriendSearchKey = ref('') // 添加好友弹窗的搜索（全网查找）
const searchResult = ref(null)
const searchLoading = ref(false)
const showVerifyInput = ref(false)
const verifyMessage = ref('您好，我想添加您为好友')
const sendingRequest = ref(false)
const inviteLink = computed(() => {
  if (!userStore.user?.id) return ''
  return `https://lantis.im/u/invite/${userStore.user.id}`
})

const createGroupForm = reactive({
  name: '',
  description: '',
  joinType: 'FREE',
  maxMembers: 200,
  avatar: ''
})

const createGroupFormRef = ref(null)
const avatarInput = ref(null)
const memberSearchKeyword = ref('')
const selectedMembers = reactive(new Map())
const memberSourceTab = ref('friends')
const selectedSourceGroup = ref(null)
const sourceGroupMembers = ref([])
const orgCheckedKeys = ref([])
const orgTreeData = computed(() => {
  // TODO: 将 organizationMembers 转换为树形结构
  return []
})

// 组织相关
const hasOrganization = computed(() => organizationMembers.value.length > 0)
const currentOrganization = ref(null)
const showCreateOrgDialog = ref(false)
const showJoinOrgDialog = ref(false)
const showInviteMemberDialog = ref(false)
const creatingOrg = ref(false)
const orgLogoInput = ref(null)

const createOrgForm = reactive({
  name: '',
  logo: '',
  industry: '',
  scale: ''
})

const joinOrgTab = ref('search')
const orgSearchKeyword = ref('')
const orgSearchResult = ref(null)
const searchingOrg = ref(false)
const pendingInvites = ref([])

const inviteMemberTab = ref('link')
const orgInviteLink = computed(() => {
  if (!currentOrganization.value?.id) return ''
  return `https://lantis.im/org/join/${currentOrganization.value.id}`
})

const addMemberForm = reactive({
  name: '',
  contact: ''
})

const invitedFriends = reactive(new Map())

// 群组详情相关
const showGroupDetailDialog = ref(false)
const currentGroup = ref(null)
const groupMembers = ref([])
const loadingMembers = ref(false)
const memberPage = ref(1)
const memberPageSize = ref(20)

// 编辑群组相关
const showEditGroupDialog = ref(false)
const updateGroupLoading = ref(false)
const editGroupForm = reactive({
  name: '',
  description: '',
  announcement: '',
  joinType: 'FREE'
})
const editGroupFormRef = ref(null)

// 是否是群主
const isGroupOwner = computed(() => {
  if (!currentGroup.value || !userStore.user) return false
  return currentGroup.value.ownerId === userStore.user.id
})

// 是否是管理员（包括群主），用于控制部分操作权限
const isGroupAdmin = computed(() => {
  if (!currentGroup.value || !userStore.user) return false
  if (currentGroup.value.ownerId === userStore.user.id) return true
  const me = groupMembers.value.find(m => String(m.userId) === String(userStore.user.id))
  return !!me && me.role === 'ADMIN'
})

// 分类数据
const categories = ref([
  { key: 'friends', name: '我的好友', icon: 'UserFilled', count: 0 },
  { key: 'organization', name: '组织架构', icon: 'OfficeBuilding', count: 0 },
  { key: 'groups', name: '我的群组', icon: 'User', count: 0 },
  { key: 'requests', name: '好友申请', icon: 'Message', count: 0 }
])

// 好友列表
const friends = ref([])
const groups = ref([])
const friendRequests = ref([])
const organizationTree = ref([])
const organizationMembers = ref([]) // 组织架构成员列表
const showColleagueDrawer = ref(false)
const selectedColleague = ref(null)
const loadingGroups = ref(false)

const treeProps = {
  children: 'children',
  label: 'name'
}

// 过滤后的好友列表（本地过滤）
const filteredFriends = computed(() => {
  if (!friendListFilterKey.value) return friends.value
  
  const keyword = friendListFilterKey.value.toLowerCase()
  return friends.value.filter(friend => 
    friend.nickname.toLowerCase().includes(keyword) ||
    friend.department?.toLowerCase().includes(keyword)
  )
})

// 菜单选择处理
const handleMenuSelect = ({ key }) => {
  activeCategory.value = key
  selectedKeys.value = [key]
  
  // 清理之前的 Lottie 实例
  destroyAllLottie()
  
  if (key === 'friends') {
    loadFriends()
  } else if (key === 'organization') {
    loadOrganization()
    nextTick(() => initOrgLottie())
  } else if (key === 'groups') {
    loadGroups()
    nextTick(() => initGroupsLottie())
  } else if (key === 'requests') {
    loadFriendRequests()
    nextTick(() => initRequestsLottie())
  }
}

// 初始化组织架构空状态 Lottie
const initOrgLottie = () => {
  if (organizationTree.value.length === 0 && orgEmptyLottieContainer.value && !orgLottieInstance) {
    orgLottieInstance = lottie.loadAnimation({
      container: orgEmptyLottieContainer.value,
      renderer: 'svg',
      loop: false,
      autoplay: true,
      animationData: emptyAnimationData
    })
  }
}

// 初始化群组空状态 Lottie
const initGroupsLottie = () => {
  if (groups.value.length === 0 && groupsEmptyLottieContainer.value && !groupsLottieInstance) {
    groupsLottieInstance = lottie.loadAnimation({
      container: groupsEmptyLottieContainer.value,
      renderer: 'svg',
      loop: true,
      autoplay: true,
      animationData: emptyAnimationData
    })
  }
}

// 初始化好友申请空状态 Lottie
const initRequestsLottie = () => {
  if (friendRequests.value.length === 0 && requestsEmptyLottieContainer.value && !requestsLottieInstance) {
    requestsLottieInstance = lottie.loadAnimation({
      container: requestsEmptyLottieContainer.value,
      renderer: 'svg',
      loop: false,
      autoplay: true,
      animationData: emptyAnimationData
    })
  }
}

// 销毁所有 Lottie 实例
const destroyAllLottie = () => {
  if (orgLottieInstance) {
    orgLottieInstance.destroy()
    orgLottieInstance = null
  }
  if (groupsLottieInstance) {
    groupsLottieInstance.destroy()
    groupsLottieInstance = null
  }
  if (requestsLottieInstance) {
    requestsLottieInstance.destroy()
    requestsLottieInstance = null
  }
}

// 设置/取消管理员
const handleSetAdmin = async (member, makeAdmin) => {
  if (!currentGroup.value) return

  const action = makeAdmin ? 'ADD' : 'REMOVE'

  try {
    const res = await groupAPI.setAdmin(currentGroup.value.id, {
      userId: member.userId,
      action
    })

    if (res.code === 200) {
      ElMessage.success(makeAdmin ? '已设为管理员' : '已取消管理员')
      await loadGroupMembers(currentGroup.value.id)
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('设置管理员失败:', error)
    ElMessage.error('操作失败')
  }
}

// 移除成员
const handleRemoveMember = async (member) => {
  if (!currentGroup.value) return

  try {
    const res = await groupAPI.removeMember(currentGroup.value.id, member.userId)
    if (res.code === 200) {
      ElMessage.success('已移除成员')
      await loadGroupMembers(currentGroup.value.id)
    } else {
      ElMessage.error(res.message || '移除失败')
    }
  } catch (error) {
    console.error('移除成员失败:', error)
    ElMessage.error('移除失败')
  }
}

// 加载好友列表
const loadFriends = async () => {
  try {
    const response = await friendAPI.getFriends(userStore.user.id)
    if (response.code === 200 && response.data) {
      friends.value = response.data
    } else {
      friends.value = []
    }
    categories.value[0].count = friends.value.length
  } catch (error) {
    console.error('加载好友列表失败:', error)
  }
}

// 加载组织架构
const loadOrganization = async () => {
  try {
    const response = await organizationAPI.getOrganizationTree()
    if (response && response.data) {
      organizationTree.value = response.data
      // 将树形结构展开为列表
      organizationMembers.value = flattenOrgTree(response.data)
    }
  } catch (error) {
    console.error('加载组织架构失败:', error)
  }
}

// 将组织架构树展开为列表
const flattenOrgTree = (tree) => {
  const result = []
  const traverse = (nodes) => {
    nodes.forEach(node => {
      if (node.type === 'user') {
        result.push({
          id: node.id,
          name: node.name,
          avatar: node.avatar,
          department: node.department || '未分配部门',
          position: node.position || '员工',
          email: node.email,
          phone: node.phone,
          employeeId: node.employeeId
        })
      }
      if (node.children && node.children.length > 0) {
        traverse(node.children)
      }
    })
  }
  traverse(tree)
  return result
}

// 加载群组列表
const loadGroups = async () => {
  // 防止重复加载
  if (loadingGroups.value) return
  
  try {
    if (!userStore.user || !userStore.user.id) {
      console.error('用户信息缺失')
      groups.value = []
      categories.value[2].count = 0
      return
    }

    loadingGroups.value = true
    const response = await groupAPI.getUserGroups(userStore.user.id, { page: 1, size: 50 })
    const groupList = response.data?.groups || []

    groups.value = groupList.map(group => ({
      id: group.groupId,
      name: group.name,
      avatar: group.avatar || '',
      description: group.description || '暂无描述',
      announcement: group.announcement || '',
      memberCount: group.memberCount || 0,
      maxMembers: group.maxMembers || 500,
      joinType: group.joinType || 'FREE',
      ownerId: group.ownerId,
      createdAt: group.createdAt,
      updatedAt: group.updatedAt,
      lastActiveTime: dayjs(group.updatedAt || group.createdAt || Date.now()).valueOf()
    }))

    categories.value[2].count = groups.value.length
    
    // 如果没有群组，初始化 Lottie 动画
    nextTick(() => {
      if (groups.value.length === 0) {
        initGroupsLottie()
      }
    })
  } catch (error) {
    console.error('加载群组列表失败:', error)
    groups.value = []
    categories.value[2].count = 0
    
    // 加载失败也显示动画
    nextTick(() => initGroupsLottie())
  } finally {
    loadingGroups.value = false
  }
}

// 加载好友申请
const loadFriendRequests = async () => {
  try {
    const response = await friendAPI.getReceivedRequests(userStore.user.id)
    if (response.code === 200 && response.data) {
      friendRequests.value = response.data
    } else {
      friendRequests.value = []
    }
    categories.value[3].count = friendRequests.value.length
  } catch (error) {
    console.error('加载好友申请失败:', error)
    friendRequests.value = []
  }
}

// 搜索用户（新版 - 全网查找）
const handleSearchUser = async () => {
  if (!addFriendSearchKey.value.trim()) {
    ElMessage.warning('请输入邮箱、手机号或Lantis号')
    return
  }

  try {
    searchLoading.value = true
    searchResult.value = null
    showVerifyInput.value = false

    const response = await userAPI.searchUser(addFriendSearchKey.value.trim())
    
    if (response.code === 200 && response.data) {
      searchResult.value = response.data
    } else {
      ElMessage.warning('未找到该用户')
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    searchLoading.value = false
  }
}

// 发送好友请求（新版）
const handleSendRequest = async () => {
  if (!searchResult.value) return

  try {
    sendingRequest.value = true

    const response = await friendAPI.sendRequest({
      toUserId: searchResult.value.id,
      message: verifyMessage.value || '您好，我想添加您为好友'
    })

    if (response.code === 200) {
      ElMessage.success('好友请求已发送')
      showAddFriendDialog.value = false
      // 重置状态
      addFriendSearchKey.value = ''
      searchResult.value = null
      showVerifyInput.value = false
      verifyMessage.value = '您好，我想添加您为好友'
    } else {
      ElMessage.error(response.message || '发送失败')
    }
  } catch (error) {
    console.error('发送好友请求失败:', error)
    ElMessage.error('发送失败')
  } finally {
    sendingRequest.value = false
  }
}

// 复制邀请链接
const handleCopyLink = async () => {
  try {
    await navigator.clipboard.writeText(inviteLink.value)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
  }
}

// 重置邀请链接
const handleResetLink = () => {
  ElMessageBox.confirm(
    '重置后旧链接将失效，确定要重置吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // TODO: 调用后端API重置链接
    ElMessage.success('链接已重置')
  }).catch(() => {
    // 取消操作
  })
}

// 搜索用户（旧版，保留兼容）
const searchUsers = async () => {
  if (!addFriendForm.keyword.trim()) {
    ElMessage.warning('请输入邮箱地址')
    return
  }
  
  try {
    const response = await friendAPI.searchUser('email', addFriendForm.keyword)
    
    if (response.code === 200 && response.data) {
      // 将单个用户包装成数组
      searchResults.value = [{
        id: response.data.id,
        nickname: response.data.displayName || response.data.username,
        avatar: response.data.avatarUrl,
        email: response.data.email
      }]
    } else {
      searchResults.value = []
      ElMessage.info('未找到匹配的用户')
    }
  } catch (error) {
    console.error('搜索用户失败:', error)
    searchResults.value = []
    ElMessage.error('搜索失败')
  }
}

// 发送好友申请
const sendFriendRequest = async (user) => {
  try {
    const response = await friendAPI.sendFriendRequest({
      fromUserId: userStore.user.id,
      toUserId: user.id,
      message: addFriendForm.message
    })
    
    if (response.code === 200) {
      ElMessage.success('好友请求已发送，等待对方接受')
      showAddFriendDialog.value = false
      searchResults.value = []
      addFriendForm.keyword = ''
      addFriendForm.message = ''
    } else {
      ElMessage.error(response.message || '发送好友请求失败')
    }
  } catch (error) {
    console.error('发送好友请求失败:', error)
    ElMessage.error('发送好友请求失败')
  }
}

// 处理好友申请
const handleFriendRequest = async (request, action) => {
  try {
    if (action === 'accept') {
      await friendAPI.acceptFriendRequest(request.fromUserId, userStore.user.id)
      ElMessage.success('已同意好友申请')
      // 刷新好友列表
      await loadFriends()
    } else {
      await friendAPI.rejectFriendRequest(request.fromUserId, userStore.user.id)
      ElMessage.success('已拒绝好友申请')
    }
    
    // 刷新好友请求列表
    await loadFriendRequests()
  } catch (error) {
    console.error('处理好友申请失败:', error)
    ElMessage.error('操作失败')
  }
}

// 开始聊天
const startChat = (friend) => {
  // 构建会话ID（单聊格式：较小ID-较大ID）
  const userId1 = Math.min(userStore.user.id, friend.userId)
  const userId2 = Math.max(userStore.user.id, friend.userId)
  const conversationId = `${userId1}-${userId2}`
  
  // 跳转到消息页面，并传递会话信息
  router.push({
    path: '/im/messages',
    query: {
      conversationId: conversationId,
      friendId: friend.userId,
      friendName: friend.nickname || friend.username
    }
  })
}

// 拨打电话
const makeCall = (friend) => {
  ElMessage.info('语音通话功能开发中...')
}

// 查看联系人详情
const viewContact = (contact) => {
  ElMessage.info('联系人详情功能开发中...')
}

// 查看群组详情
const viewGroup = async (group) => {
  try {
    currentGroup.value = group
    showGroupDetailDialog.value = true
    
    // 加载群成员
    await loadGroupMembers(group.id)
  } catch (error) {
    console.error('加载群组详情失败:', error)
    ElMessage.error('无法加载群组详情')
  }
}

// 加载群成员
const loadGroupMembers = async (groupId) => {
  try {
    loadingMembers.value = true
    memberPage.value = 1
    
    const response = await groupAPI.getMembers(groupId, {
      page: memberPage.value,
      size: memberPageSize.value
    })
    
    if (response.code === 200 && response.data) {
      groupMembers.value = response.data.members || response.data.records || []
    }
  } catch (error) {
    console.error('加载群成员失败:', error)
  } finally {
    loadingMembers.value = false
  }
}

// 加载更多成员
const loadMoreMembers = async () => {
  if (!currentGroup.value) return
  
  try {
    loadingMembers.value = true
    memberPage.value++
    
    const response = await groupAPI.getMembers(currentGroup.value.id, {
      page: memberPage.value,
      size: memberPageSize.value
    })
    
    if (response.code === 200 && response.data) {
      const newMembers = response.data.members || response.data.records || []
      groupMembers.value = [...groupMembers.value, ...newMembers]
    }
  } catch (error) {
    console.error('加载更多成员失败:', error)
  } finally {
    loadingMembers.value = false
  }
}

// 从详情页进入群聊
const enterGroupFromDetail = () => {
  if (!currentGroup.value) return
  showGroupDetailDialog.value = false
  enterGroup(currentGroup.value)
}

// 获取加入方式文本
const getJoinTypeText = (joinType) => {
  const typeMap = {
    'FREE': '自由加入',
    'APPROVAL': '需要审批',
    'INVITE': '仅邀请'
  }
  return typeMap[joinType] || joinType
}

// 编辑群组（仅群主）- 打开编辑对话框
const showEditGroup = () => {
  if (!currentGroup.value) return
  
  // 填充表单数据
  editGroupForm.name = currentGroup.value.name || ''
  editGroupForm.description = currentGroup.value.description || ''
  editGroupForm.announcement = currentGroup.value.announcement || ''
  editGroupForm.joinType = currentGroup.value.joinType || 'FREE'
  
  showEditGroupDialog.value = true
}

// 处理更新群组
const handleUpdateGroup = async () => {
  // 验证
  if (!editGroupForm.name || editGroupForm.name.trim().length < 2) {
    ElMessage.warning('请输入群组名称（至少2个字符）')
    return
  }
  
  if (!currentGroup.value) {
    ElMessage.error('群组信息缺失')
    return
  }
  
  try {
    updateGroupLoading.value = true
    
    const requestData = {
      name: editGroupForm.name.trim(),
      description: editGroupForm.description.trim() || null,
      announcement: editGroupForm.announcement.trim() || null,
      joinType: editGroupForm.joinType
    }
    
    const response = await groupAPI.updateGroup(currentGroup.value.id, requestData)
    
    if (response.code === 200) {
      ElMessage.success('群组信息已更新！')
      showEditGroupDialog.value = false
      
      // 更新当前群组信息
      if (response.data) {
        currentGroup.value = {
          ...currentGroup.value,
          ...response.data
        }
      }
      
      // 刷新群组列表
      await loadGroups()
    } else {
      ElMessage.error(response.message || '更新失败')
    }
  } catch (error) {
    console.error('更新群组失败:', error)
    ElMessage.error('更新失败: ' + (error.message || '网络错误'))
  } finally {
    updateGroupLoading.value = false
  }
}

// 确认退出群组
const confirmLeaveGroup = () => {
  ElMessageBox.confirm(
    '确定要退出该群组吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    await handleLeaveGroup()
  }).catch(() => {})
}

// 处理退出群组
const handleLeaveGroup = async () => {
  if (!currentGroup.value || !userStore.user) return
  
  try {
    const response = await groupAPI.leaveGroup(
      currentGroup.value.id,
      userStore.user.id
    )
    
    if (response.code === 200) {
      ElMessage.success('已退出群组')
      showGroupDetailDialog.value = false
      await loadGroups()
    } else {
      ElMessage.error(response.message || '退出失败')
    }
  } catch (error) {
    console.error('退出群组失败:', error)
    ElMessage.error('退出失败')
  }
}

// 确认解散群组（仅群主）
const confirmDissolveGroup = () => {
  ElMessageBox.confirm(
    '解散群组后将无法恢复，确定要解散吗？',
    '警告',
    {
      confirmButtonText: '确定解散',
      cancelButtonText: '取消',
      type: 'error',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    await handleDissolveGroup()
  }).catch(() => {})
}

// 处理解散群组
const handleDissolveGroup = async () => {
  if (!currentGroup.value) return
  
  try {
    const response = await groupAPI.dissolveGroup(currentGroup.value.id)
    
    if (response.code === 200) {
      ElMessage.success('群组已解散')
      showGroupDetailDialog.value = false
      await loadGroups()
    } else {
      ElMessage.error(response.message || '解散失败')
    }
  } catch (error) {
    console.error('解散群组失败:', error)
    ElMessage.error('解散失败')
  }
}

// 进入群聊
const enterGroup = (group) => {
  router.push({
    path: '/im/messages',
    query: { type: 'group', groupId: group.id }
  })
}

// 获取头像文字（根据群名生成）
const getAvatarText = (name) => {
  if (!name) return '群'
  return name.length > 2 ? name.substring(0, 2) : name
}

// 更新头像预览
const updateAvatarPreview = () => {
  // 头像文字会自动更新
}

// 获取组织Logo文字
const getOrgLogoText = (name) => {
  if (!name) return '企'
  return name.length > 2 ? name.substring(0, 2) : name
}

// 更新组织Logo文字
const updateOrgLogoText = () => {
  // Logo文字会自动更新
}

// 点击组织Logo上传
const handleOrgLogoClick = () => {
  orgLogoInput.value?.click()
}

// 处理组织Logo上传
const handleOrgLogoChange = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    createOrgForm.logo = e.target.result
  }
  reader.readAsDataURL(file)
}

// 点击头像上传
const handleAvatarClick = () => {
  avatarInput.value?.click()
}

// 处理头像上传
const handleAvatarChange = (event) => {
  const file = event.target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }

  // 验证文件大小（限制5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过5MB')
    return
  }

  // 读取并预览图片
  const reader = new FileReader()
  reader.onload = (e) => {
    createGroupForm.avatar = e.target.result
  }
  reader.readAsDataURL(file)
}

// 切换成员选择
const toggleCreateMember = (friend) => {
  if (selectedMembers.has(friend.userId)) {
    selectedMembers.delete(friend.userId)
  } else {
    selectedMembers.set(friend.userId, friend)
  }
}

// 移除成员
const removeMember = (userId) => {
  selectedMembers.delete(userId)
}

// 过滤好友列表
const filteredFriendsForCreate = computed(() => {
  if (!memberSearchKeyword.value) return friends.value
  const keyword = memberSearchKeyword.value.toLowerCase()
  return friends.value.filter(f =>
    f.nickname?.toLowerCase().includes(keyword)
  )
})

// 进入群组（查看成员）
const enterSourceGroup = async (group) => {
  selectedSourceGroup.value = group
  // TODO: 加载群成员
  sourceGroupMembers.value = []
}

// 处理组织架构勾选
const handleOrgCheck = (checkedKeys, e) => {
  orgCheckedKeys.value = checkedKeys
  // TODO: 将勾选的组织成员添加到 selectedMembers
}

// 创建群组 - 打开弹窗
const createGroup = () => {
  // 重置表单
  createGroupForm.name = ''
  createGroupForm.description = ''
  createGroupForm.avatar = ''
  createGroupForm.joinType = 'FREE'
  createGroupForm.maxMembers = 100
  showCreateGroupDialog.value = true
}

// 处理创建群组
const handleCreateGroup = async () => {
  // 验证
  if (!createGroupForm.name || createGroupForm.name.trim().length < 2) {
    ElMessage.warning('请输入群组名称（至少2个字符）')
    return
  }
  
  if (!userStore.user || !userStore.user.id) {
    ElMessage.error('用户信息缺失，请重新登录')
    return
  }
  
  try {
    createGroupLoading.value = true
    
    // 收集选中的成员ID
    const memberIds = Array.from(selectedMembers.keys())
    
    const requestData = {
      name: createGroupForm.name.trim(),
      description: createGroupForm.description.trim() || null,
      joinType: createGroupForm.joinType,
      maxMembers: createGroupForm.maxMembers,
      ownerId: userStore.user.id,
      avatar: createGroupForm.avatar || null,
      memberIds: memberIds // 包含选中的成员
    }
    
    const response = await groupAPI.createGroup(requestData)
    
    if (response.code === 200) {
      const memberCount = memberIds.length
      const message = memberCount > 0 
        ? `群组创建成功！已邀请 ${memberCount} 位成员` 
        : '群组创建成功！'
      ElMessage.success(message)
      showCreateGroupDialog.value = false
      
      // 清空选中的成员
      selectedMembers.clear()
      memberSearchKeyword.value = ''
      
      // 刷新群组列表
      await loadGroups()
      
      // 切换到群组标签
      activeCategory.value = 'groups'
    } else {
      ElMessage.error(response.message || '创建失败')
    }
  } catch (error) {
    console.error('创建群组失败:', error)
    ElMessage.error('创建失败: ' + (error.message || '网络错误'))
  } finally {
    createGroupLoading.value = false
  }
}

// 刷新组织架构
const refreshOrganization = () => {
  loadOrganization()
  ElMessage.success('已刷新')
}

// 处理树节点点击
const handleNodeClick = (data) => {
  if (data.type === 'user') {
    viewContact(data)
  }
}

// 搜索处理
const handleSearch = () => {
  // 搜索逻辑已在computed中处理
}

// 获取状态颜色 (Ant Design 风格)
const getStatusColor = (status) => {
  const map = {
    'online': 'success',
    'busy': 'warning',
    'away': 'default',
    'offline': 'default'
  }
  return map[status] || 'default'
}

// 获取状态文本
const getStatusText = (status) => {
  const map = {
    'online': '在线',
    'busy': '忙碌',
    'away': '离开',
    'offline': '离线'
  }
  return map[status] || '未知'
}

// 格式化时间
const formatTime = (timestamp) => {
  return dayjs(timestamp).format('MM-DD HH:mm')
}

// 定时器ID
let pollingTimer = null

onMounted(() => {
  // 检查登录状态
  if (!userStore.isLoggedIn || !userStore.user) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  
  // 初始加载
  loadFriends()
  loadFriendRequests()
  
  // 每30秒轮询一次好友请求
  pollingTimer = setInterval(() => {
    loadFriendRequests()
  }, 30000)
})

onUnmounted(() => {
  // 清除定时器
  if (pollingTimer) {
    clearInterval(pollingTimer)
  }
  
  // 清理 Lottie 实例
  destroyAllLottie()
})
</script>

<style scoped>
.contacts-container {
  height: 100%;
  background: #f5f6f7;
}

.contacts-layout {
  height: 100%;
  background: #f5f6f7;
}

/* 左侧导航栏 */
.contacts-sider {
  background: #fff !important;
  border-right: 1px solid #e5e7eb;
}

.sider-header {
  padding: 20px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.sider-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1f2329;
}

.search-section {
  padding: 16px;
}

.contact-menu {
  border-right: none;
}

.menu-count {
  margin-left: auto;
  font-size: 12px;
  color: #8f959e;
  background: #f2f3f5;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 右侧内容区域 */
.contacts-content {
  padding: 24px 32px;
  overflow-y: auto;
  background: #f5f6f7;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.header-left h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1f2329;
}

.count-text {
  font-size: 16px;
  color: #8f959e;
  margin-left: 8px;
}

.header-right {
  display: flex;
  align-items: center;
}

/* 好友列表视图 */
.friend-list {
  background: #fff;
  border-radius: 8px;
  padding: 8px 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.friend-row {
  display: flex;
  align-items: center;
  padding: 10px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.friend-row:last-child {
  border-bottom: none;
}

.friend-row:hover {
  background-color: #f5f7fa;
}

.row-avatar {
  margin-right: 16px;
  flex-shrink: 0;
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.row-info {
  flex: 1;
  min-width: 0;
}

.row-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.row-badge {
  font-size: 11px;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
}

.row-desc {
  font-size: 12px;
  color: #8f959e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-top: 2px;
}

.row-actions {
  margin-left: auto;
  display: none;
  gap: 8px;
  flex-shrink: 0;
}

.friend-row:hover .row-actions {
  display: flex;
}

.row-actions .icon-btn {
  width: 32px;
  height: 32px;
  color: #606266;
  transition: all 0.2s ease;
}

.row-actions .icon-btn:hover {
  background: #e1eaff;
  color: #4E59CC;
}

/* 好友网格 */
.contacts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.contact-card {
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.contact-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 8px 0;
}

.contact-avatar {
  margin-bottom: 16px;
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.contact-info {
  width: 100%;
  margin-bottom: 16px;
}

.contact-name {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
}

.contact-dept {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #8f959e;
}

.status-tag {
  font-size: 12px;
}

.contact-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

/* 组织架构空状态 - 行动卡片 */
.org-onboarding-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 40px;
  min-height: 70vh;
  padding: 40px;
}

.action-card {
  width: 300px;
  text-align: center;
  padding: 48px 32px;
  border-radius: 12px;
  background: #fff;
  transition: all 0.3s ease;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.action-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.15);
}

.primary-card {
  border: 2px solid transparent;
}

.primary-card:hover {
  border-color: #4E59CC;
}

.secondary-card {
  border: 2px solid #E1EAFF;
}

.secondary-card:hover {
  border-color: #4E59CC;
}

.icon-large {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #E8F3FF 0%, #F0F6FF 100%);
  color: #4E59CC;
  font-size: 52px;
  box-shadow: 0 4px 12px rgba(78, 89, 204, 0.15);
}

.card-title {
  font-size: 20px;
  font-weight: 700;
  color: #1F2329;
  margin: 0 0 12px 0;
}

.card-description {
  font-size: 13px;
  color: #8F959E;
  line-height: 1.6;
  margin: 0 0 28px 0;
  min-height: 42px;
}

.card-divider {
  width: 1px;
  height: 200px;
  background: linear-gradient(to bottom, 
    transparent 0%, 
    #E5E7EB 20%, 
    #E5E7EB 80%, 
    transparent 100%
  );
}

/* 次级按钮样式 */
.secondary-button {
  border: 2px solid #4E59CC !important;
  color: #4E59CC !important;
  background: transparent !important;
}

.secondary-button:hover {
  background: #F0F3FF !important;
  border-color: #4E59CC !important;
}

/* 组织架构 */
.org-tree-card {
  border-radius: 8px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-icon {
  color: #5B7ADB;
  font-size: 16px;
}

.node-avatar {
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.node-label {
  font-size: 14px;
  color: #1f2329;
  font-weight: 500;
}

.member-count {
  font-size: 12px;
  color: #8f959e;
  margin-left: 8px;
}

.position-tag {
  margin-left: auto;
}

/* 群组列表 */
.groups-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.group-card {
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.group-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.group-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.group-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.group-info {
  flex: 1;
  min-width: 0;
}

.group-header-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.group-name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1f2329;
}

.member-count-badge {
  font-size: 12px;
  color: #8f959e;
  background: #f2f3f5;
  padding: 2px 8px;
  border-radius: 10px;
}

.group-desc {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #8f959e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.group-meta {
  display: flex;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #8f959e;
}

.group-actions {
  flex-shrink: 0;
}

/* 好友申请 */
.requests-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.request-card {
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.03);
}

.request-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.request-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.request-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, #5B7ADB 0%, #4E59CC 100%);
}

.request-info {
  flex: 1;
  min-width: 0;
}

.request-name {
  margin: 0 0 8px 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}

.request-message {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #8f959e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.request-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #8f959e;
}

.request-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

/* 空状态 */
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.empty-lottie {
  width: 220px;
  height: 220px;
}

.empty-text {
  margin: 16px 0 24px 0;
  font-size: 14px;
  color: #8f959e;
}

.search-results {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.search-results h4 {
  margin: 0 0 12px 0;
  color: #333;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.user-info {
  flex: 1;
}

.user-info h5 {
  margin: 0 0 2px 0;
  color: #333;
}

.user-info p {
  margin: 0;
  color: #666;
  font-size: 12px;
}

/* 群组详情样式 */
.group-detail {
  padding: 12px 0;
}

.group-header {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding-bottom: 20px;
}

.group-header .group-info {
  flex: 1;
}

.group-header h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: #333;
}

.group-header .description {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
}

.group-header .group-meta {
  display: flex;
  gap: 8px;
}

.member-list {
  max-height: 300px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  transition: background 0.2s;
}

.member-item:hover {
  background: #f5f5f5;
}

.member-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.member-name {
  color: #333;
  font-size: 14px;
}

.group-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

/* 群公告样式 */
.group-announcement {
  margin-bottom: 20px;
}

.announcement-content {
  padding: 12px;
  background: #fff9e6;
  border-left: 3px solid #faad14;
  border-radius: 4px;
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 群组骨架屏样式 */
.groups-skeleton {
  padding: 24px;
}

/* 创建群组对话框样式 - 飞书风格 */
.create-group-layout {
  display: flex;
  gap: 1px;
  background: #f0f0f0;
  min-height: 480px;
}

.group-info-section {
  flex: 0 0 280px;
  background: #fff;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.avatar-upload-section {
  display: flex;
  justify-content: center;
}

/* 现代化头像上传器 */
.avatar-uploader-modern {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar-uploader-modern:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.avatar-placeholder-modern {
  width: 100%;
  height: 100%;
  background: #F5F6F7;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #C1C4C9;
}

.camera-icon-modern {
  font-size: 32px;
  margin-bottom: 8px;
}

.avatar-text-modern {
  font-size: 24px;
  font-weight: 600;
  color: #8F959E;
}

.avatar-preview-modern {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-hover-mask-modern {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 13px;
  gap: 6px;
}

.avatar-hover-mask-modern .anticon {
  font-size: 24px;
}

.avatar-uploader-modern:hover .avatar-hover-mask-modern {
  opacity: 1;
}

.group-form {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

.form-item.required label::before {
  content: '* ';
  color: #F53F3F;
  margin-right: 4px;
  font-weight: 700;
}

.divider-vertical {
  width: 1px;
  background: #f0f0f0;
}

.member-select-section {
  flex: 1;
  background: #FAFAFA;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1F2329;
  margin-bottom: 16px;
}

.member-count {
  margin-left: auto;
  font-size: 12px;
  color: #8F959E;
  font-weight: normal;
}

.mini-member-selector {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-search {
  flex-shrink: 0;
  margin-bottom: 12px;
}

/* Tab 样式优化 */
.member-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}

.member-tabs :deep(.ant-tabs-nav-wrap::after) {
  height: 1px;
  background-color: #F0F0F0;
}

.member-tabs :deep(.ant-tabs-tab) {
  font-size: 13px;
  color: #606266;
  padding: 8px 0;
}

.member-tabs :deep(.ant-tabs-tab.ant-tabs-tab-active) {
  color: #4E59CC;
  font-weight: 600;
}

.member-tabs :deep(.ant-tabs-ink-bar) {
  background: #4E59CC;
}

/* 列表容器 */
.member-list {
  max-height: 320px;
  overflow-y: auto;
  padding-right: 4px;
}

/* 列表项美化 - 精修版 */
.member-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 52px;
  padding: 0 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.member-list-item:hover {
  background: #F5F7FA;
}

/* 选中状态 */
.member-list-item:has(.ant-checkbox-checked) {
  background: #ECF5FF;
}

.member-list-item:has(.ant-checkbox-checked):hover {
  background: #E1EAFF;
}

/* 双行布局 - 飞书风格 */
.info-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  margin-left: 12px;
}

.name-text {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub-text {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 组织架构树 */
.org-tree-container {
  max-height: 320px;
  overflow-y: auto;
}

.tree-node-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 群组列表 */
.group-list {
  max-height: 320px;
  overflow-y: auto;
}

.group-list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.group-list-item:hover {
  background: #F5F7FA;
}

.group-info {
  flex: 1;
}

.group-name {
  font-size: 14px;
  color: #1F2329;
  margin-bottom: 4px;
}

.group-count {
  font-size: 12px;
  color: #8F959E;
}

.arrow-icon {
  font-size: 12px;
  color: #C1C4C9;
}

/* 群成员 */
.back-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  margin-bottom: 12px;
  color: #4E59CC;
  cursor: pointer;
  font-size: 13px;
  border-radius: 6px;
  transition: all 0.2s;
}

.back-header:hover {
  background: #F5F7FA;
}

/* 已选成员区域 - 终极优化版 */
.selected-footer-pro {
  margin-top: auto;
  border-top: 1px solid #F0F0F0;
  padding: 12px 0;
  min-height: 60px;
}

.footer-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
  font-weight: 400;
}

.avatar-row {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  height: 32px;
  align-items: center;
  /* 隐藏滚动条 */
  scrollbar-width: none;
}

.avatar-row::-webkit-scrollbar {
  display: none;
}

.avatar-wrapper {
  position: relative;
  width: 32px;
  height: 32px;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}

.avatar-wrapper:hover {
  transform: scale(1.08);
}

/* 删除遮罩 - 默认隐藏 */
.close-badge {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  color: #fff;
  font-size: 16px;
  display: none;
  align-items: center;
  justify-content: center;
  font-weight: 400;
}

.avatar-wrapper:hover .close-badge {
  display: flex;
}

.member-tooltip {
  position: absolute;
  bottom: -30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.2s;
  z-index: 10;
}

.avatar-wrapper:hover .member-tooltip {
  opacity: 1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 0 0 0;
  border-top: 1px solid #f0f0f0;
  margin-top: 16px;
}

/* 添加好友对话框样式 - 飞书风格 */
.add-friend-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 24px;
}

.tab-content {
  min-height: 300px;
  padding: 8px 0;
}

.search-box {
  margin-bottom: 20px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #F5F6F7;
  border-radius: 8px;
  margin-bottom: 16px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #1F2329;
  margin-bottom: 4px;
}

.user-desc {
  font-size: 13px;
  color: #8F959E;
}

.verify-area {
  background: #fff;
  border: 1px solid #DEE0E3;
  border-radius: 8px;
  padding: 16px;
}

.verify-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.verify-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #8F959E;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: #C1C4C9;
}

/* 链接邀请样式 */
.invite-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.qr-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 32px;
}

.qr-code {
  width: 180px;
  height: 180px;
  background: #F5F6F7;
  border: 1px solid #DEE0E3;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
}

.qr-placeholder {
  font-size: 64px;
  color: #C1C4C9;
  margin-bottom: 8px;
}

.qr-text {
  font-size: 14px;
  color: #8F959E;
}

.qr-tip {
  font-size: 13px;
  color: #8F959E;
  text-align: center;
}

.link-section {
  width: 100%;
  margin-bottom: 24px;
}

.link-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.link-row {
  display: flex;
  gap: 12px;
}

.link-input {
  flex: 1;
}

.link-tip {
  font-size: 12px;
  color: #8F959E;
  margin-top: 8px;
  text-align: center;
}

.reset-section {
  text-align: center;
}

/* 创建组织对话框 - 专业版 */
.create-org-dialog :deep(.ant-modal-header) {
  border-bottom: 1px solid #F0F0F0;
  padding: 20px 24px;
}

.dialog-title {
  font-size: 20px;
  font-weight: 700;
  color: #1F2329;
}

.dialog-content {
  padding: 32px 24px 24px;
}

/* Logo上传区域 - 顶部居中 */
.org-logo-uploader {
  display: flex;
  justify-content: center;
  margin-bottom: 32px;
}

.logo-upload-area {
  position: relative;
  width: 96px;
  height: 96px;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
}

.logo-upload-area:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.logo-placeholder-pro {
  width: 100%;
  height: 100%;
  background: #FAFAFA;
  border: 2px dashed #D9D9D9;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #C1C4C9;
  transition: all 0.3s;
}

.logo-upload-area:hover .logo-placeholder-pro {
  border-color: #4E59CC;
  background: #F0F6FF;
}

.camera-icon-pro {
  font-size: 32px;
  margin-bottom: 8px;
}

.logo-text-pro {
  font-size: 24px;
  font-weight: 600;
  color: #8F959E;
}

.logo-preview-pro {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.logo-hover-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
  font-size: 13px;
  gap: 6px;
}

.logo-hover-overlay .anticon {
  font-size: 24px;
}

.logo-upload-area:hover .logo-hover-overlay {
  opacity: 1;
}

/* 表单区域 */
.org-form-pro {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-item-pro {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.form-item-pro.required .form-label::before {
  content: '* ';
  color: #F53F3F;
  font-weight: 700;
}

/* 两列布局 */
.form-row {
  display: flex;
  gap: 20px;
}

.form-col {
  flex: 1;
}

/* 底部按钮 - 单按钮右对齐 */
.dialog-footer-single {
  display: flex;
  justify-content: flex-end;
}

.create-org-dialog :deep(.ant-modal-footer) {
  padding: 16px 24px;
  border-top: 1px solid #F0F0F0;
}

.create-org-dialog :deep(.ant-btn) {
  border-radius: 8px;
  font-weight: 500;
}

.create-org-dialog :deep(.ant-btn-default) {
  border: 1px solid #D9D9D9;
  color: #606266;
}

.create-org-dialog :deep(.ant-btn-default:hover) {
  border-color: #4E59CC;
  color: #4E59CC;
}

.create-org-dialog :deep(.ant-btn-primary) {
  background: #4E59CC;
  border-color: #4E59CC;
  box-shadow: 0 2px 4px rgba(78, 89, 204, 0.2);
}

.create-org-dialog :deep(.ant-btn-primary:hover) {
  background: #3D4AB8;
  border-color: #3D4AB8;
  box-shadow: 0 4px 8px rgba(78, 89, 204, 0.3);
}

/* 加入组织 - 搜索输入组 */
.search-input-group {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-input-group .ant-input {
  flex: 1;
}

.join-org-content {
  padding: 16px 0;
}

.org-result-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #F5F7FA;
  border-radius: 8px;
  margin-top: 16px;
}

.org-info {
  flex: 1;
}

.org-name {
  font-size: 16px;
  font-weight: 600;
  color: #1F2329;
  margin-bottom: 4px;
}

.org-desc {
  font-size: 13px;
  color: #8F959E;
}

/* 加入组织对话框 - 固定大小 */
.join-org-dialog :deep(.ant-modal-body) {
  min-height: 400px;
  display: flex;
  flex-direction: column;
}

.join-org-dialog :deep(.ant-tabs) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.join-org-dialog :deep(.ant-tabs-content-holder) {
  flex: 1;
}

.pending-invites {
  min-height: 320px;
}

/* 群组设置对话框 - 飞书风格 */
.group-setting-modal :deep(.ant-modal-header) {
  border-bottom: 1px solid #F0F0F0;
  padding: 16px 20px;
}

.group-setting-modal :deep(.ant-modal-body) {
  padding: 0;
  max-height: 70vh;
  overflow-y: auto;
}

.drawer-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

/* 区块 A：群信息概览 */
.section-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  border-bottom: 8px solid #F5F6F7;
}

.info-text {
  flex: 1;
}

.group-name-text {
  font-size: 16px;
  font-weight: 600;
  color: #1F2329;
  margin-bottom: 4px;
}

.group-desc-text {
  font-size: 12px;
  color: #8F959E;
  line-height: 1.4;
}

/* 区块 B：群成员管理 */
.section-members {
  padding: 20px;
  border-bottom: 8px solid #F5F6F7;
}

.sec-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sec-title {
  font-size: 14px;
  font-weight: 600;
  color: #1F2329;
}

.sec-count {
  font-size: 13px;
  color: #8F959E;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color 0.2s;
}

.sec-count:hover {
  color: #3370FF;
}

.member-search-input {
  margin-bottom: 16px;
  background: #F5F6F7;
  border: none;
  border-radius: 4px;
}

.member-search-input :deep(.ant-input) {
  background: #F5F6F7;
  border: none;
}

.avatar-row-preview {
  display: flex;
  gap: 8px;
  align-items: center;
}

.icon-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 16px;
}

.add-btn {
  background: #E1EAFF;
  color: #4E59CC;
}

.add-btn:hover {
  background: #D1DCFF;
}

.remove-btn {
  background: #FFE7E7;
  color: #F53F3F;
}

.remove-btn:hover {
  background: #FFD7D7;
}

/* 区块 C：功能入口列表 */
.section-list {
  padding: 0 20px;
  border-bottom: 8px solid #F5F6F7;
}

.list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 48px;
  cursor: pointer;
  border-bottom: 1px solid #F0F0F0;
  transition: background 0.2s;
}

.list-item:last-child {
  border-bottom: none;
}

.list-item:hover {
  background: #F5F7FA;
}

.list-item span {
  font-size: 14px;
  color: #1F2329;
}

.list-item .anticon {
  font-size: 12px;
  color: #8F959E;
}

/* 区块 D：群昵称设置 */
.section-nickname {
  padding: 20px;
  border-bottom: 8px solid #F5F6F7;
}

.nickname-label {
  font-size: 13px;
  color: #606266;
  margin-bottom: 8px;
}

.nickname-input {
  border-radius: 4px;
}

.nickname-input :deep(.ant-input) {
  border-color: #DCDFE6;
}

/* 区块 E：危险操作 */
.section-danger {
  padding: 20px;
  margin-top: auto;
}

.section-danger .ant-btn {
  border-radius: 6px;
  font-weight: 500;
}
</style>
