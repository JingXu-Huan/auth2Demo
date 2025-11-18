/**
 * WebSocket 全局连接管理
 */

class WebSocketService {
  constructor() {
    this.ws = null
    this.userId = null
    this.reconnectTimer = null
    this.heartbeatTimer = null
    this.messageHandlers = []
    this.isConnecting = false
  }

  /**
   * 建立 WebSocket 连接
   */
  connect(userId) {
    if (this.isConnecting || (this.ws && this.ws.readyState === WebSocket.OPEN)) {
      console.log('WebSocket 已连接或正在连接中')
      return
    }

    this.userId = userId
    this.isConnecting = true

    // 通过 IM-Gateway 连接 (端口 9001)
    const wsUrl = `ws://localhost:9001/ws/${userId}`
    console.log('正在建立 WebSocket 连接 (通过网关):', wsUrl)

    try {
      this.ws = new WebSocket(wsUrl)

      this.ws.onopen = () => {
        console.log('WebSocket 连接成功, userId:', userId)
        this.isConnecting = false
        this.startHeartbeat()
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('收到 WebSocket 消息:', data)
          
          // 通知所有注册的消息处理器
          this.messageHandlers.forEach(handler => {
            try {
              handler(data)
            } catch (error) {
              console.error('消息处理器执行失败:', error)
            }
          })
        } catch (error) {
          console.error('解析 WebSocket 消息失败:', error, event.data)
        }
      }

      this.ws.onclose = (event) => {
        console.log('WebSocket 连接关闭:', event.code, event.reason)
        this.isConnecting = false
        this.stopHeartbeat()
        
        // 非正常关闭，尝试重连
        if (event.code !== 1000 && this.userId) {
          this.reconnect()
        }
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket 错误:', error)
        this.isConnecting = false
      }
    } catch (error) {
      console.error('创建 WebSocket 连接失败:', error)
      this.isConnecting = false
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    console.log('主动断开 WebSocket 连接')
    this.stopHeartbeat()
    this.stopReconnect()
    
    if (this.ws) {
      this.ws.close(1000, 'User logout')
      this.ws = null
    }
    
    this.userId = null
    this.messageHandlers = []
  }

  /**
   * 重连
   */
  reconnect() {
    if (this.reconnectTimer) {
      return
    }

    console.log('5秒后尝试重连...')
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      if (this.userId) {
        this.connect(this.userId)
      }
    }, 5000)
  }

  /**
   * 停止重连
   */
  stopReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  /**
   * 发送消息
   */
  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const messageStr = typeof message === 'string' ? message : JSON.stringify(message)
      this.ws.send(messageStr)
      console.log('发送 WebSocket 消息:', message)
      return true
    } else {
      console.warn('WebSocket 未连接，无法发送消息')
      return false
    }
  }

  /**
   * 注册消息处理器
   */
  onMessage(handler) {
    if (typeof handler === 'function') {
      this.messageHandlers.push(handler)
    }
  }

  /**
   * 移除消息处理器
   */
  offMessage(handler) {
    const index = this.messageHandlers.indexOf(handler)
    if (index > -1) {
      this.messageHandlers.splice(index, 1)
    }
  }

  /**
   * 启动心跳
   */
  startHeartbeat() {
    this.stopHeartbeat()
    
    this.heartbeatTimer = setInterval(() => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        this.send({ type: 'ping' })
      }
    }, 30000) // 每30秒发送一次心跳
  }

  /**
   * 停止心跳
   */
  stopHeartbeat() {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  /**
   * 获取连接状态
   */
  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

// 创建全局单例
const websocketService = new WebSocketService()

export default websocketService
