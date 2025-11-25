/**
 * WebSocket 连接管理
 */
class WebSocketService {
  constructor() {
    this.ws = null
    this.userId = null
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.handlers = new Map()
    this.isConnecting = false
    this.reconnectCount = 0
    this.maxReconnect = 5
  }

  /**
   * 建立连接
   */
  connect(userId) {
    if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.OPEN)) {
      return
    }

    this.userId = userId
    this.isConnecting = true

    const wsUrl = import.meta.env.VITE_WS_BASE_URL || 'ws://localhost:9090'
    const url = `${wsUrl}/ws/${userId}`
    
    console.log('[WebSocket] 正在连接:', url)

    try {
      this.ws = new WebSocket(url)

      this.ws.onopen = () => {
        console.log('[WebSocket] 连接成功')
        this.isConnecting = false
        this.reconnectCount = 0
        this.startHeartbeat()
        this.emit('connected')
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('[WebSocket] 收到消息:', data)
          
          // 触发对应类型的处理器
          if (data.type) {
            this.emit(data.type, data)
          }
          this.emit('message', data)
        } catch (e) {
          console.error('[WebSocket] 解析消息失败:', e)
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
   * 注册事件处理器
   */
  on(event, handler) {
    if (!this.handlers.has(event)) {
      this.handlers.set(event, [])
    }
    this.handlers.get(event).push(handler)
  }

  /**
   * 移除事件处理器
   */
  off(event, handler) {
    const handlers = this.handlers.get(event)
    if (handlers) {
      const index = handlers.indexOf(handler)
      if (index > -1) {
        handlers.splice(index, 1)
      }
    }
  }

  /**
   * 触发事件
   */
  emit(event, data) {
    const handlers = this.handlers.get(event)
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
   * 心跳
   */
  startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = setInterval(() => {
      this.send({ type: 'ping' })
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
