/**
 * Protobuf 协议工具
 * 用于与 IM-gateway-netty 通信
 */
import protobuf from 'protobufjs'

// 协议常量
export const CommandType = {
  UNKNOWN: 0,
  AUTH: 1,
  HEARTBEAT: 2,
  MSG_PUSH: 3,
  MSG_ACK: 4,
  KICK_OUT: 5
}

// Protobuf 消息类型（将在 init 时初始化）
let IMPacket = null
let Header = null
let AuthRequest = null
let AuthResponse = null
let HeartbeatRequest = null
let HeartbeatResponse = null
let PushMessage = null
let MessageAck = null
let KickOutNotify = null

// 序列号计数器
let seqCounter = 0

/**
 * 初始化 Protobuf
 */
export async function initProtobuf() {
  try {
    // 使用内联定义，避免加载 .proto 文件的问题
    const root = protobuf.Root.fromJSON({
      nested: {
        im: {
          nested: {
            protocol: {
              nested: {
                IMPacket: {
                  fields: {
                    header: { type: "Header", id: 1 },
                    body: { type: "bytes", id: 2 }
                  }
                },
                Header: {
                  fields: {
                    command: { type: "int32", id: 1 },
                    version: { type: "int32", id: 2 },
                    client_id: { type: "string", id: 3 },
                    seq: { type: "int64", id: 4 },
                    timestamp: { type: "int64", id: 5 }
                  }
                },
                AuthRequest: {
                  fields: {
                    token: { type: "string", id: 1 },
                    device_id: { type: "string", id: 2 },
                    device_type: { type: "string", id: 3 }
                  }
                },
                AuthResponse: {
                  fields: {
                    success: { type: "bool", id: 1 },
                    message: { type: "string", id: 2 },
                    user_id: { type: "int64", id: 3 }
                  }
                },
                HeartbeatRequest: {
                  fields: {
                    timestamp: { type: "int64", id: 1 }
                  }
                },
                HeartbeatResponse: {
                  fields: {
                    timestamp: { type: "int64", id: 1 },
                    message: { type: "string", id: 2 }
                  }
                },
                PushMessage: {
                  fields: {
                    sender_id: { type: "int64", id: 1 },
                    receiver_id: { type: "int64", id: 2 },
                    group_id: { type: "int64", id: 3 },
                    msg_type: { type: "int32", id: 4 },
                    content: { type: "string", id: 5 },
                    msg_id: { type: "int64", id: 6 },
                    timestamp: { type: "int64", id: 7 }
                  }
                },
                MessageAck: {
                  fields: {
                    msg_id: { type: "int64", id: 1 },
                    receiver_id: { type: "int64", id: 2 },
                    timestamp: { type: "int64", id: 3 }
                  }
                },
                KickOutNotify: {
                  fields: {
                    reason: { type: "string", id: 1 },
                    timestamp: { type: "int64", id: 2 }
                  }
                }
              }
            }
          }
        }
      }
    })

    IMPacket = root.lookupType('im.protocol.IMPacket')
    Header = root.lookupType('im.protocol.Header')
    AuthRequest = root.lookupType('im.protocol.AuthRequest')
    AuthResponse = root.lookupType('im.protocol.AuthResponse')
    HeartbeatRequest = root.lookupType('im.protocol.HeartbeatRequest')
    HeartbeatResponse = root.lookupType('im.protocol.HeartbeatResponse')
    PushMessage = root.lookupType('im.protocol.PushMessage')
    MessageAck = root.lookupType('im.protocol.MessageAck')
    KickOutNotify = root.lookupType('im.protocol.KickOutNotify')

    console.log('[Protobuf] 初始化成功')
    return true
  } catch (error) {
    console.error('[Protobuf] 初始化失败:', error)
    return false
  }
}

// 存储当前用户ID（必须在 generateDeviceId 之前定义）
let currentUserId = null

/**
 * 设置当前用户ID（用于开发模式）
 */
export function setCurrentUserId(userId) {
  currentUserId = userId
  console.log('[Protobuf] setCurrentUserId:', userId)
}

/**
 * 生成设备ID
 * 开发模式下使用 DEV_{userId}_{timestamp} 格式
 */
function generateDeviceId(userId) {
  console.log('[Protobuf] generateDeviceId called with userId:', userId, 'currentUserId:', currentUserId)
  
  // 使用传入的 userId 或全局 currentUserId
  const effectiveUserId = userId || currentUserId
  
  if (effectiveUserId) {
    // 开发模式：使用 DEV_{userId}_{timestamp} 格式
    const devId = `DEV_${effectiveUserId}_${Date.now()}`
    console.log('[Protobuf] 生成开发模式 deviceId:', devId)
    return devId
  }
  
  let deviceId = localStorage.getItem('deviceId')
  if (!deviceId) {
    deviceId = 'WEB_' + Date.now() + '_' + Math.random().toString(36).substring(2, 9)
    localStorage.setItem('deviceId', deviceId)
  }
  console.log('[Protobuf] 使用普通 deviceId:', deviceId)
  return deviceId
}

/**
 * 创建消息头
 */
function createHeader(command) {
  return Header.create({
    command: command,
    version: 1,
    client_id: generateDeviceId(currentUserId),
    seq: ++seqCounter,
    timestamp: Date.now()
  })
}

/**
 * 创建认证请求包
 */
export function createAuthPacket(token) {
  const header = createHeader(CommandType.AUTH)
  const authRequest = AuthRequest.create({
    token: token || '',
    device_id: generateDeviceId(currentUserId),
    device_type: 'WEB'
  })
  
  const packet = IMPacket.create({
    header: header,
    body: AuthRequest.encode(authRequest).finish()
  })
  
  return IMPacket.encode(packet).finish()
}

/**
 * 创建心跳请求包
 */
export function createHeartbeatPacket() {
  const header = createHeader(CommandType.HEARTBEAT)
  const heartbeatRequest = HeartbeatRequest.create({
    timestamp: Date.now()
  })
  
  const packet = IMPacket.create({
    header: header,
    body: HeartbeatRequest.encode(heartbeatRequest).finish()
  })
  
  return IMPacket.encode(packet).finish()
}

/**
 * 创建消息确认包
 */
export function createAckPacket(msgId, receiverId) {
  const header = createHeader(CommandType.MSG_ACK)
  const ack = MessageAck.create({
    msg_id: msgId,
    receiver_id: receiverId,
    timestamp: Date.now()
  })
  
  const packet = IMPacket.create({
    header: header,
    body: MessageAck.encode(ack).finish()
  })
  
  return IMPacket.encode(packet).finish()
}

/**
 * 解析收到的数据包
 */
export function parsePacket(data) {
  try {
    // 打印原始数据用于调试
    const bytes = new Uint8Array(data)
    console.log('[Protobuf] 收到数据, 长度:', bytes.length, '前20字节:', Array.from(bytes.slice(0, 20)))
    
    // 检查是否是 Protobuf 消息
    if (!IMPacket) {
      console.error('[Protobuf] IMPacket 未初始化')
      return null
    }
    
    const packetMsg = IMPacket.decode(bytes)
    // 转换为普通对象，更容易处理
    const packet = IMPacket.toObject(packetMsg, {
      longs: Number,
      defaults: true
    })
    console.log('[Protobuf] 解析后 packet:', JSON.stringify(packet))
    
    if (!packet || !packet.header) {
      console.error('[Protobuf] 解析后 packet 或 header 为空, packet:', packet)
      return null
    }
    
    const command = packet.header.command
    console.log('[Protobuf] 解析成功, command:', command)
    
    // 使用原始消息的 body 进行解码
    const bodyBytes = packetMsg.body
    let body = null
    
    if (bodyBytes && bodyBytes.length > 0) {
      try {
        switch (command) {
          case CommandType.AUTH:
            body = AuthResponse.toObject(AuthResponse.decode(bodyBytes), { longs: Number })
            break
          case CommandType.HEARTBEAT:
            body = HeartbeatResponse.toObject(HeartbeatResponse.decode(bodyBytes), { longs: Number })
            break
          case CommandType.MSG_PUSH:
            body = PushMessage.toObject(PushMessage.decode(bodyBytes), { longs: Number })
            break
          case CommandType.MSG_ACK:
            body = MessageAck.toObject(MessageAck.decode(bodyBytes), { longs: Number })
            break
          case CommandType.KICK_OUT:
            body = KickOutNotify.toObject(KickOutNotify.decode(bodyBytes), { longs: Number })
            break
          default:
            console.warn('[Protobuf] 未知命令类型:', command)
        }
      } catch (bodyError) {
        console.error('[Protobuf] 解析 body 失败:', bodyError)
      }
    }
    
    return {
      header: packet.header,
      command: command,
      body: body
    }
  } catch (error) {
    console.error('[Protobuf] 解析数据包失败:', error)
    // 尝试作为文本解析
    try {
      const text = new TextDecoder().decode(data)
      console.log('[Protobuf] 尝试作为文本解析:', text)
    } catch (e) {
      // 忽略
    }
    return null
  }
}

export default {
  CommandType,
  initProtobuf,
  setCurrentUserId,
  createAuthPacket,
  createHeartbeatPacket,
  createAckPacket,
  parsePacket
}
