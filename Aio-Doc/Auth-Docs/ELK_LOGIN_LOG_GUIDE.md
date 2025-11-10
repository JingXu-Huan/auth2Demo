# 📊 ELK 登录日志收集指南

**版本**: v1.0  
**日期**: 2025-11-10

---

## 🎯 概述

登录日志已改为**结构化日志输出**，由 ELK 收集和分析，不再存储到数据库。

---

## 📝 日志格式

### 1. 登录成功日志

```
LOGIN_SUCCESS|email=user@example.com|ip=192.168.1.100|userAgent=Mozilla/5.0...|device=DESKTOP
```

**字段说明**:
- `email`: 用户邮箱
- `ip`: 客户端IP（支持代理）
- `userAgent`: 浏览器信息
- `device`: 设备类型（MOBILE/TABLET/DESKTOP）

### 2. 登录失败日志

```
LOGIN_FAILURE|email=user@example.com|ip=192.168.1.100|userAgent=Mozilla/5.0...|device=DESKTOP|reason=Bad credentials|remainingAttempts=4
```

**字段说明**:
- `email`: 尝试登录的邮箱
- `ip`: 客户端IP
- `userAgent`: 浏览器信息
- `device`: 设备类型
- `reason`: 失败原因
- `remainingAttempts`: 剩余尝试次数

---

## 🔧 Logstash 配置

### logstash.conf

```ruby
input {
  # 从 Filebeat 接收日志
  beats {
    port => 5044
  }
}

filter {
  # 解析登录成功日志
  if [message] =~ /LOGIN_SUCCESS/ {
    grok {
      match => {
        "message" => "LOGIN_SUCCESS\|email=%{DATA:email}\|ip=%{IP:client_ip}\|userAgent=%{DATA:user_agent}\|device=%{WORD:device_type}"
      }
    }
    
    mutate {
      add_field => {
        "login_status" => "SUCCESS"
        "event_type" => "login"
      }
    }
    
    # 解析 User-Agent
    useragent {
      source => "user_agent"
      target => "ua"
    }
    
    # GeoIP 解析
    geoip {
      source => "client_ip"
      target => "geo"
    }
  }
  
  # 解析登录失败日志
  if [message] =~ /LOGIN_FAILURE/ {
    grok {
      match => {
        "message" => "LOGIN_FAILURE\|email=%{DATA:email}\|ip=%{IP:client_ip}\|userAgent=%{DATA:user_agent}\|device=%{WORD:device_type}\|reason=%{DATA:failure_reason}\|remainingAttempts=%{NUMBER:remaining_attempts:int}"
      }
    }
    
    mutate {
      add_field => {
        "login_status" => "FAILED"
        "event_type" => "login"
      }
    }
    
    # 解析 User-Agent
    useragent {
      source => "user_agent"
      target => "ua"
    }
    
    # GeoIP 解析
    geoip {
      source => "client_ip"
      target => "geo"
    }
  }
  
  # 添加时间戳
  date {
    match => [ "timestamp", "ISO8601" ]
    target => "@timestamp"
  }
}

output {
  # 输出到 Elasticsearch
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
    index => "login-logs-%{+YYYY.MM.dd}"
    document_type => "_doc"
  }
  
  # 调试输出（可选）
  # stdout { codec => rubydebug }
}
```

---

## 📦 Filebeat 配置

### filebeat.yml

```yaml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      # Spring Boot 日志路径
      - /var/log/oauth2-auth-server/*.log
    
    # 多行日志合并
    multiline.pattern: '^[0-9]{4}-[0-9]{2}-[0-9]{2}'
    multiline.negate: true
    multiline.match: after
    
    # 只收集登录日志
    include_lines: ['LOGIN_SUCCESS', 'LOGIN_FAILURE']
    
    # 添加字段
    fields:
      service: oauth2-auth-server
      environment: production

output.logstash:
  hosts: ["logstash:5044"]
```

---

## 📊 Elasticsearch 索引模板

### login-logs-template.json

```json
{
  "index_patterns": ["login-logs-*"],
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 1,
    "index.lifecycle.name": "login-logs-policy",
    "index.lifecycle.rollover_alias": "login-logs"
  },
  "mappings": {
    "properties": {
      "@timestamp": { "type": "date" },
      "email": { "type": "keyword" },
      "client_ip": { "type": "ip" },
      "user_agent": { "type": "text" },
      "device_type": { "type": "keyword" },
      "login_status": { "type": "keyword" },
      "failure_reason": { "type": "text" },
      "remaining_attempts": { "type": "integer" },
      "geo": {
        "properties": {
          "location": { "type": "geo_point" },
          "country_name": { "type": "keyword" },
          "city_name": { "type": "keyword" }
        }
      },
      "ua": {
        "properties": {
          "name": { "type": "keyword" },
          "os": { "type": "keyword" },
          "device": { "type": "keyword" }
        }
      }
    }
  }
}
```

**创建模板**:
```bash
curl -X PUT "http://elasticsearch:9200/_index_template/login-logs-template" \
  -H 'Content-Type: application/json' \
  -d @login-logs-template.json
```

---

## 🎨 Kibana 可视化

### 1. 创建索引模式

```
Management → Index Patterns → Create index pattern
Index pattern: login-logs-*
Time field: @timestamp
```

### 2. 常用查询

#### 查询登录成功记录
```
login_status: "SUCCESS"
```

#### 查询登录失败记录
```
login_status: "FAILED"
```

#### 查询特定用户
```
email: "user@example.com"
```

#### 查询特定IP
```
client_ip: "192.168.1.100"
```

#### 查询移动设备登录
```
device_type: "MOBILE"
```

### 3. 可视化面板

#### 登录趋势图（折线图）
```
Visualization Type: Line
Metrics: Count
Buckets: Date Histogram (@timestamp)
Split Series: login_status
```

#### 设备类型分布（饼图）
```
Visualization Type: Pie
Metrics: Count
Buckets: Terms (device_type)
```

#### 地理位置分布（地图）
```
Visualization Type: Maps
Layer: Documents
Index pattern: login-logs-*
Geospatial field: geo.location
```

#### 失败原因统计（条形图）
```
Visualization Type: Horizontal Bar
Metrics: Count
Buckets: Terms (failure_reason.keyword)
Filter: login_status: "FAILED"
```

---

## 🔍 常用分析查询

### 1. 登录成功率

```json
GET /login-logs-*/_search
{
  "size": 0,
  "aggs": {
    "login_stats": {
      "terms": {
        "field": "login_status"
      }
    }
  }
}
```

### 2. 每小时登录量

```json
GET /login-logs-*/_search
{
  "size": 0,
  "aggs": {
    "logins_per_hour": {
      "date_histogram": {
        "field": "@timestamp",
        "calendar_interval": "hour"
      },
      "aggs": {
        "by_status": {
          "terms": {
            "field": "login_status"
          }
        }
      }
    }
  }
}
```

### 3. 异常IP检测（短时间多次失败）

```json
GET /login-logs-*/_search
{
  "size": 0,
  "query": {
    "bool": {
      "must": [
        { "term": { "login_status": "FAILED" } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  },
  "aggs": {
    "suspicious_ips": {
      "terms": {
        "field": "client_ip",
        "min_doc_count": 5,
        "size": 10
      }
    }
  }
}
```

### 4. 新设备登录检测

```json
GET /login-logs-*/_search
{
  "size": 100,
  "query": {
    "bool": {
      "must": [
        { "term": { "login_status": "SUCCESS" } },
        { "range": { "@timestamp": { "gte": "now-24h" } } }
      ]
    }
  },
  "aggs": {
    "users": {
      "terms": {
        "field": "email",
        "size": 100
      },
      "aggs": {
        "devices": {
          "cardinality": {
            "field": "device_type"
          }
        }
      }
    }
  }
}
```

---

## 🚨 告警配置

### Elasticsearch Watcher 告警

#### 1. 登录失败率过高告警

```json
PUT _watcher/watch/high_failure_rate
{
  "trigger": {
    "schedule": {
      "interval": "5m"
    }
  },
  "input": {
    "search": {
      "request": {
        "indices": ["login-logs-*"],
        "body": {
          "query": {
            "range": {
              "@timestamp": {
                "gte": "now-5m"
              }
            }
          },
          "aggs": {
            "status": {
              "terms": {
                "field": "login_status"
              }
            }
          }
        }
      }
    }
  },
  "condition": {
    "script": {
      "source": "def failed = ctx.payload.aggregations.status.buckets.find(b -> b.key == 'FAILED'); def total = ctx.payload.hits.total.value; return failed != null && total > 0 && (failed.doc_count / total) > 0.3"
    }
  },
  "actions": {
    "email_admin": {
      "email": {
        "to": "admin@example.com",
        "subject": "⚠️ 登录失败率过高告警",
        "body": "最近5分钟登录失败率超过30%，请检查系统！"
      }
    }
  }
}
```

#### 2. 异常IP告警

```json
PUT _watcher/watch/suspicious_ip
{
  "trigger": {
    "schedule": {
      "interval": "10m"
    }
  },
  "input": {
    "search": {
      "request": {
        "indices": ["login-logs-*"],
        "body": {
          "query": {
            "bool": {
              "must": [
                { "term": { "login_status": "FAILED" } },
                { "range": { "@timestamp": { "gte": "now-10m" } } }
              ]
            }
          },
          "aggs": {
            "ips": {
              "terms": {
                "field": "client_ip",
                "min_doc_count": 10
              }
            }
          }
        }
      }
    }
  },
  "condition": {
    "compare": {
      "ctx.payload.aggregations.ips.buckets.0.doc_count": {
        "gte": 10
      }
    }
  },
  "actions": {
    "email_security": {
      "email": {
        "to": "security@example.com",
        "subject": "🚨 检测到异常IP",
        "body": "IP {{ctx.payload.aggregations.ips.buckets.0.key}} 在10分钟内失败{{ctx.payload.aggregations.ips.buckets.0.doc_count}}次"
      }
    }
  }
}
```

---

## 📈 性能优化

### 1. 索引生命周期管理

```json
PUT _ilm/policy/login-logs-policy
{
  "policy": {
    "phases": {
      "hot": {
        "actions": {
          "rollover": {
            "max_size": "50GB",
            "max_age": "7d"
          }
        }
      },
      "warm": {
        "min_age": "7d",
        "actions": {
          "shrink": {
            "number_of_shards": 1
          },
          "forcemerge": {
            "max_num_segments": 1
          }
        }
      },
      "delete": {
        "min_age": "90d",
        "actions": {
          "delete": {}
        }
      }
    }
  }
}
```

### 2. 数据保留策略

- **Hot 阶段**: 最近 7 天，快速查询
- **Warm 阶段**: 7-90 天，压缩存储
- **Delete 阶段**: 90 天后自动删除

---

## 🎯 总结

### 优势

1. ✅ **无数据库压力** - 不占用数据库资源
2. ✅ **强大分析能力** - Kibana 可视化
3. ✅ **地理位置分析** - GeoIP 支持
4. ✅ **实时告警** - Watcher 监控
5. ✅ **自动清理** - ILM 生命周期管理

### 日志示例

**登录成功**:
```
2025-11-10 17:00:00.123 INFO  LOGIN_SUCCESS|email=user@example.com|ip=192.168.1.100|userAgent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)|device=DESKTOP
```

**登录失败**:
```
2025-11-10 17:00:05.456 WARN  LOGIN_FAILURE|email=user@example.com|ip=192.168.1.100|userAgent=Mozilla/5.0 (iPhone; CPU iPhone OS 14_0)|device=MOBILE|reason=Bad credentials|remainingAttempts=4
```

---

**ELK 配置完成！** 🎉

现在登录日志会自动被 ELK 收集、分析和可视化！
