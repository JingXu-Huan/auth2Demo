# 📡 API 接口文档

## 目录

- [用户注册登录](#用户注册登录)
- [邮箱验证](#邮箱验证)
- [安全验证](#安全验证)
- [OAuth2 认证](#oauth2-认证)
- [用户管理](#用户管理)

---

## 用户注册登录

### 1. 用户注册

**接口**: `POST /api/users/register`

**请求参数**:
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Test@123"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "username": "testuser",
    "email": "test@example.com",
    "emailVerified": false
  }
}
```

**密码要求**:
- 至少 8 位字符
- 必须包含大写字母
- 必须包含小写字母
- 必须包含数字
- 必须包含特殊字符 (@$!%*?&)

**错误码**:
- `400`: 参数错误
- `409`: 邮箱已存在
- `500`: 服务器错误

---

### 2. 用户登录

**接口**: `POST /oauth/token`

**请求头**:
```
Content-Type: application/x-www-form-urlencoded
```

**请求参数**:
```
grant_type=password
username=test@example.com
password=Test@123
client_id=client
client_secret=secret
```

**响应示例**:
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 7199,
  "scope": "read write"
}
```

**登录流程**:
1. 检查是否需要安全验证 (`GET /api/security/check`)
2. 如需验证，完成安全验证流程
3. 提交登录请求
4. 获取 Token

**错误码**:
- `400`: 参数错误
- `401`: 用户名或密码错误
- `403`: 账户被锁定
- `423`: 邮箱未验证

---

### 3. 刷新 Token

**接口**: `POST /oauth/token`

**请求参数**:
```
grant_type=refresh_token
refresh_token=YOUR_REFRESH_TOKEN
client_id=client
client_secret=secret
```

**响应示例**:
```json
{
  "access_token": "NEW_ACCESS_TOKEN",
  "token_type": "bearer",
  "refresh_token": "NEW_REFRESH_TOKEN",
  "expires_in": 7199,
  "scope": "read write"
}
```

---

## 邮箱验证

### 1. 发送验证码

**接口**: `POST /api/email/send-code`

**请求参数**:
```json
{
  "email": "test@example.com"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "验证码已发送",
  "data": null
}
```

**限制**:
- 同一邮箱 5 分钟内只能发送一次
- 验证码有效期 5 分钟

**错误码**:
- `400`: 邮箱格式错误
- `429`: 请求过于频繁
- `500`: 发送失败

---

### 2. 验证邮箱验证码

**接口**: `POST /api/email/verify-code`

**请求参数**:
```json
{
  "email": "test@example.com",
  "code": "123456"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "验证成功",
  "data": true
}
```

**错误码**:
- `400`: 验证码错误或已过期
- `500`: 验证失败

---

### 3. 验证并激活账户

**接口**: `POST /api/email/verify-and-activate`

**请求参数**:
```json
{
  "email": "test@example.com",
  "code": "123456"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "邮箱验证成功，账户已激活",
  "data": null
}
```

**说明**: 
- 用户注册后必须调用此接口激活账户
- 激活后才能登录

---

## 安全验证

### 1. 检查是否需要安全验证

**接口**: `GET /api/security/check`

**请求参数**:
```
?email=test@example.com
```

**响应示例**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "needsVerification": true,
    "daysSinceLastLogin": 35,
    "message": "您已超过30天未登录，需要进行安全验证"
  }
}
```

**说明**:
- 超过 30 天未登录需要安全验证
- 验证通过后 15 分钟内可登录

---

### 2. 发送安全验证码

**接口**: `POST /api/security/send-code`

**请求参数**:
```json
{
  "email": "test@example.com"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "安全验证码已发送",
  "data": null
}
```

---

### 3. 验证安全验证码

**接口**: `POST /api/security/verify-code`

**请求参数**:
```json
{
  "email": "test@example.com",
  "code": "123456"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "验证成功，15分钟内可以登录",
  "data": true
}
```

---

## OAuth2 认证

### 1. 获取授权码

**接口**: `GET /oauth/authorize`

**请求参数**:
```
?response_type=code
&client_id=client
&redirect_uri=http://localhost:3000/callback
&scope=read write
&state=random_state
```

**响应**: 重定向到登录页面

---

### 2. 授权码换取 Token

**接口**: `POST /oauth/token`

**请求参数**:
```
grant_type=authorization_code
code=AUTHORIZATION_CODE
redirect_uri=http://localhost:3000/callback
client_id=client
client_secret=secret
```

**响应示例**:
```json
{
  "access_token": "ACCESS_TOKEN",
  "token_type": "bearer",
  "refresh_token": "REFRESH_TOKEN",
  "expires_in": 7199,
  "scope": "read write"
}
```

---

### 3. Gitee 第三方登录

#### 3.1 发起授权

**接口**: `GET /oauth/gitee/authorize`

**响应**: 重定向到 Gitee 授权页面

#### 3.2 授权回调

**接口**: `GET /oauth/gitee/callback`

**请求参数**:
```
?code=GITEE_CODE
&state=STATE
```

**响应**: 重定向到前端，携带 Token

---

## 用户管理

### 1. 获取用户信息

**接口**: `GET /api/users/{userId}`

**请求头**:
```
Authorization: Bearer ACCESS_TOKEN
```

**响应示例**:
```json
{
  "code": 200,
  "message": "成功",
  "data": {
    "userId": 1,
    "username": "testuser",
    "email": "test@example.com",
    "displayName": "测试用户",
    "avatarUrl": "https://example.com/avatar.jpg",
    "createdAt": "2025-11-10T10:00:00"
  }
}
```

---

### 2. 修改密码

**接口**: `POST /api/users/{userId}/change-password`

**请求头**:
```
Authorization: Bearer ACCESS_TOKEN
```

**请求参数**:
```json
{
  "oldPassword": "Test@123",
  "newPassword": "NewTest@456"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "密码修改成功",
  "data": null
}
```

**新密码要求**:
- 符合强密码规则
- 不能与最近 5 次使用的密码相同

---

### 3. 检查邮箱是否存在

**接口**: `GET /api/users/exists/email/{email}`

**响应示例**:
```json
{
  "code": 200,
  "message": "成功",
  "data": true
}
```

---

### 4. 检查用户名是否存在

**接口**: `GET /api/users/exists/username/{username}`

**响应示例**:
```json
{
  "code": 200,
  "message": "成功",
  "data": false
}
```

---

## 通用响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "成功",
  "data": {
    // 业务数据
  }
}
```

### 错误响应

```json
{
  "code": 400,
  "message": "参数错误",
  "data": null
}
```

---

## 错误码说明

| 错误码 | 说明 |
|-------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 423 | 资源被锁定 |
| 429 | 请求过于频繁 |
| 500 | 服务器错误 |

---

## 请求示例

### cURL

```bash
# 用户注册
curl -X POST http://localhost:9000/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Test@123"
  }'

# 用户登录
curl -X POST http://localhost:9000/oauth/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=test@example.com&password=Test@123&client_id=client&client_secret=secret"

# 获取用户信息
curl -X GET http://localhost:9000/api/users/1 \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### JavaScript (Axios)

```javascript
// 用户注册
axios.post('http://localhost:9000/api/users/register', {
  username: 'testuser',
  email: 'test@example.com',
  password: 'Test@123'
})
.then(response => console.log(response.data))
.catch(error => console.error(error));

// 用户登录
const params = new URLSearchParams();
params.append('grant_type', 'password');
params.append('username', 'test@example.com');
params.append('password', 'Test@123');
params.append('client_id', 'client');
params.append('client_secret', 'secret');

axios.post('http://localhost:9000/oauth/token', params)
.then(response => {
  const token = response.data.access_token;
  localStorage.setItem('token', token);
})
.catch(error => console.error(error));

// 获取用户信息
axios.get('http://localhost:9000/api/users/1', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
})
.then(response => console.log(response.data))
.catch(error => console.error(error));
```

---

## 完整登录流程示例

```javascript
// 1. 检查是否需要安全验证
async function checkSecurityVerification(email) {
  const response = await axios.get(`/api/security/check?email=${email}`);
  return response.data.data.needsVerification;
}

// 2. 发送安全验证码（如需要）
async function sendSecurityCode(email) {
  await axios.post('/api/security/send-code', { email });
}

// 3. 验证安全验证码（如需要）
async function verifySecurityCode(email, code) {
  const response = await axios.post('/api/security/verify-code', { email, code });
  return response.data.data;
}

// 4. 登录
async function login(email, password) {
  // 检查是否需要安全验证
  const needsVerification = await checkSecurityVerification(email);
  
  if (needsVerification) {
    // 发送验证码
    await sendSecurityCode(email);
    
    // 提示用户输入验证码
    const code = prompt('请输入验证码');
    
    // 验证验证码
    const verified = await verifySecurityCode(email, code);
    if (!verified) {
      throw new Error('验证码错误');
    }
  }
  
  // 执行登录
  const params = new URLSearchParams();
  params.append('grant_type', 'password');
  params.append('username', email);
  params.append('password', password);
  params.append('client_id', 'client');
  params.append('client_secret', 'secret');
  
  const response = await axios.post('/oauth/token', params);
  return response.data.access_token;
}

// 使用示例
login('test@example.com', 'Test@123')
  .then(token => {
    console.log('登录成功', token);
    localStorage.setItem('token', token);
  })
  .catch(error => {
    console.error('登录失败', error);
  });
```

---

## 更新日志

- **2025-11-10**: 初始版本
- 添加完整 API 文档
- 添加请求示例
- 添加错误码说明
