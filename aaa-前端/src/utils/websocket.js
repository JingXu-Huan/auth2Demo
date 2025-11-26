/**
 * WebSocket 服务（Protobuf 版本）
 * 用于与 IM-gateway-netty 建立长连接
 */
import { 
  initProtobuf, 
  setCurrentUserId,
  createAuthPacket, 
  createHeartbeatPacket, 
  createAckPacket, 
  parsePacket,
  CommandType 
} from './protobuf'

class WebSocketService {
  constructor() {
    this.ws = null
    this.userId = null
    this.reconnectCount = 0
    this.maxReconnect = 5
    this.heartbeatTimer = null
    this.isConnecting = false
    this.handlers = {}
    this.protobufReady = false
    this.pendingConnect = null
  }

  /**
   * 建立连接
   */
  async connect(userId) {
    if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.OPEN)) {
      return
    }

    this.userId = userId
    this.isConnecting = true

    // 初始化 Protobuf
    if (!this.protobufReady) {
      this.protobufReady = await initProtobuf()
      if (!this.protobufReady) {
        console.error('[WebSocket] Protobuf 初始化失败')
        this.isConnecting = false
        return
      }
    }
    
    // 设置当前用户ID（用于开发模式）
    setCurrentUserId(userId)

    // IM-gateway-netty 的 WebSocket 端口是 9090，路径是 /ws
    const wsUrl = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:9090'
    const url = `${wsUrl}/ws`
    
    console.log('[WebSocket] 正在连接:', url)

    try {
      this.ws = new WebSocket(url)
      this.ws.binaryType = 'arraybuffer'  // 使用二进制模式

      this.ws.onopen = () => {
        console.log('[WebSocket] 连接成功，发送认证请求')
        this.isConnecting = false
        this.reconnectCount = 0
        
        // 发送认证请求
        this.sendAuth()
      }

      this.ws.onmessage = (event) => {
        try {
          const packet = parsePacket(event.data)
          if (!packet || packet.command === undefined) {
            console.warn('[WebSocket] 解析数据包失败或命令为空')
            return
          }
          
          console.log('[WebSocket] 收到消息, command:', packet.command, 'body:', packet.body)
          
          // 根据命令类型处理
          switch (packet.command) {
            case CommandType.AUTH:
              if (packet.body) {
                this.handleAuthResponse(packet.body)
              }
              break
            case CommandType.HEARTBEAT:
              console.log('[WebSocket] 收到心跳响应')
              break
            case CommandType.MSG_PUSH:
              if (packet.body) {
                this.handlePushMessage(packet.body)
              }
              break
            case CommandType.KICK_OUT:
              if (packet.body) {
                this.handleKickOut(packet.body)
              }
              break
            default:
              console.log('[WebSocket] 未知命令类型:', packet.command)
          }
        } catch (e) {
          console.error('[WebSocket] 处理消息失败:', e)
        }
      }

      this.ws.onclose = (event) => {
        console.log('[WebSocket] 连接关闭:', event.code, event.reason)
        this.isConnecting = false
        this.stopHeartbeat()
        this.emit('disconnected')
        
        // 非正常关闭，尝试重连
        if (event.code !== 1000 && this.userId && this.reconnectCount < this.maxReconnect) {
          this.scheduleReconnect()
        }
      }

      this.ws.onerror = (error) => {
        console.error('[WebSocket] 错误:', error)
        this.isConnecting = false
        this.emit('error', error)
      }
    } catch (error) {
      console.error('[WebSocket] 创建连接失败:', error)
      this.isConnecting = false
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    this.stopHeartbeat()
    this.stopReconnect()
    
    if (this.ws) {
      this.ws.close(1000, '主动断开')
      this.ws = null
    }
    
    this.userId = null
    this.reconnectCount = 0
  }

  /**
   * 发送消息
   */
  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const message = typeof data === 'string' ? data : JSON.stringify(data)
      this.ws.send(message)
      return true
    }
    console.warn('[WebSocket] 未连接，无法发送消息')
    return false
  }

  /**
   * 发送认证请求
   */
  sendAuth() {
    const token = localStorage.getItem('token') || ''
    const packet = createAuthPacket(token)
    this.sendBinary(packet)
    console.log('[WebSocket] 已发送认证请求')
  }

  /**
   * 发送二进制数据
   */
  sendBinary(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(data)
      return true
    }
    console.warn('[WebSocket] 未连接，无法发送消息')
    return false
  }

  /**
   * 处理认证响应
   */
  handleAuthResponse(authResponse) {
    if (authResponse.success) {
      console.log('[WebSocket] 认证成功, userId:', authResponse.user_id)
      this.startHeartbeat()
      this.emit('connected')
    } else {
      console.error('[WebSocket] 认证失败:', authResponse.message)
      this.emit('auth_failed', authResponse.message)
      this.ws.close(4001, '认证失败')
    }
  }

  /**
   * 处理推送消息
   */
  handlePushMessage(pushMessage) {
    // 转换为前端消息格式（字段名使用 snake_case）
    const message = {
      messageId: pushMessage.msg_id,
      channelId: pushMessage.group_id,  // group_id 存储的是 channelId
      senderId: pushMessage.sender_id,
      receiverId: pushMessage.receiver_id,
      msgType: pushMessage.msg_type,
      content: pushMessage.content,
      createdAt: new Date(Number(pushMessage.timestamp)).toISOString()
    }
    
    console.log('[WebSocket] 收到推送消息:', message)
    this.emit('message', message)
  }

  /**
   * 处理踢下线通知
   */
  handleKickOut(kickOutNotify) {
    console.warn('[WebSocket] 被踢下线:', kickOutNotify.reason)
    this.emit('kick_out', kickOutNotify.reason)
    this.disconnect()
  }

  /**
   * 注册事件处理器
   */
  on(event, handler) {
    if (!this.handlers[event]) {
      this.handlers[event] = []
    }
    // 防止重复注册同一个处理器
    if (!this.handlers[event].includes(handler)) {
      this.handlers[event].push(handler)
    }
    console.log(`[WebSocket] 注册事件 ${event}, 当前处理器数量: ${this.handlers[event].length}`)
  }

  /**
   * 移除事件处理器
   */
  off(event, handler) {
    const handlers = this.handlers[event]
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
        console.log(`[WebSocket] 移除事件 ${event}, 剩余处理器数量: ${handlers.length}`)
      }
    }
  }
  
  /**
   * 清除指定事件的所有处理器
   */
  offAll(event) {
    if (event) {
      this.handlers[event] = []
    } else {
      this.handlers = {}
    }
  }

  /**
   * 触发事件
   */
  emit(event, data) {
    const handlers = this.handlers[event]
    if (handlers) {
      handlers.forEach(handler => {
        try {
          handler(data)
        } catch (e) {
          console.error('[WebSocket] 事件处理错误:', e)
        }
      })
    }
  }

  /**
   * 心跳（Protobuf 格式）
   */
  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      const packet = createHeartbeatPacket()
      this.sendBinary(packet)
    }, 30000)
  }

  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 重连
   */
  scheduleReconnect() {
    this.stopReconnect()
    this.reconnectCount++
    
    const delay = Math.min(1000 * Math.pow(2, this.reconnectCount), 30000)
    console.log(`[WebSocket] ${delay/1000}秒后重连...`)
    
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      if (this.userId) {
        this.connect(this.userId)
      }
    }, delay)
  }

  stopReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  /**
   * 连接状态
   */
  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

export default new WebSocketService()
