# 失物招领系统 - 答辩高频问题与回答指南

---

## 目录

1. [项目介绍类](#一项目介绍类)
2. [技术选型与架构类](#二技术选型与架构类)
3. [认证安全类（JWT + RBAC）](#三认证安全类jwt--rbac)
4. [核心功能实现思路类](#四核心功能实现思路类)
5. [WebSocket 实时通信类](#五websocket-实时通信类)
6. [数据库设计类](#六数据库设计类)
7. [难点与亮点类](#七难点与亮点类)
8. [优化与展望类](#八优化与展望类)

---

## 一、项目介绍类

### Q1: 请简单介绍一下你的项目

**回答模板：**

> 本项目是一个**校园失物招领管理平台**，采用前后端分离架构，前端使用 **Vue 3 + TypeScript + Element Plus**，后端使用 **Spring Boot 2.7 + MyBatis-Plus**，数据库使用 **MySQL 8**。
>
> 核心功能包括：
> - **失物/拾物发布与管理**：支持图片上传、位置标注、分类筛选
> - **智能匹配算法**：自动推荐可能对应的失物和拾物
> - **实时聊天系统**：基于 WebSocket 的即时通讯
> - **好友系统**：添加好友、备注管理、私聊、删除好友
> - **认领流程**：申请→审核→确认→归还的完整闭环
> - **管理后台**：用户管理、物品审核、公告发布、数据统计
>
> 项目解决了传统校园失物招领方式**信息传播慢、匹配效率低、缺乏实时交互**的痛点。

### Q2: 你在这个项目中担任什么角色？负责哪些模块？

**回答模板：**

> 我是**独立全栈开发者**，负责了从需求分析、数据库设计、后端API开发、前端页面实现到部署测试的全部工作。具体包括：
> - 后端：14个Controller、Service层业务逻辑、JWT认证、WebSocket实时推送
> - 前端：11个页面路由、状态管理、组件开发、Canvas图片裁剪器手写实现
> - 数据库：11张表的设计与关联关系

---

## 二、技术选型与架构类

### Q3: 为什么选择 Vue 3 而不是 React？

**回答模板：**

> 选择 Vue 3 主要基于以下考虑：
>
> | 对比维度 | Vue 3 | React |
> |---------|-------|-------|
> | **学习曲线** | 平缓，模板语法接近HTML | 较陡，需要理解JSX |
> | **生态配套** | Element Plus 组件库成熟完善 | 需要选择Ant Design/MUI等 |
> | **TypeScript支持** | 原生支持，类型推导优秀 | 支持，但配置相对复杂 |
> | **Composition API** | `setup` 语法糖简洁直观 | Hooks 需要手动管理依赖数组 |
> | **响应式系统** | Proxy-based，性能好且易用 | 需要显式声明依赖 |
>
> 对于本项目这种**中等复杂度的管理系统**，Vue 3 + Element Plus 的组合能**快速搭建出美观且功能完善的界面**，开发效率更高。

### Q4: 为什么用 MyBatis-Plus 而不是 JPA 或 MyBatis 原生？

**回答模板：**

> 本项目采用了 **MyBatis-Plus + JPA 混合方案**，各有分工：
>
> - **MyBatis-Plus 用于业务表**（lost_item, found_item, friendship, chat_message 等）
>   - 内置 CRUD 操作，无需手写 XML 映射文件
>   - 支持分页插件、条件构造器（QueryWrapper），代码量减少 60%+
>   - 复杂查询可以灵活扩展自定义 SQL
>
> - **JPA/Hibernate 用于基础实体**（User, Role）
>   - 自动 DDL 建表，适合结构稳定的用户权限模块
>   - @ManyToMany 注解处理 user_roles 关联非常方便
>
> 这样既享受了 MyBatis-Plus 的**开发效率**，又利用了 JPA 的**自动化能力**。

### Q5: 为什么选择 Spring Boot 而不是其他框架（如 Node.js / Go）？

**回答模板：**

> 选择 Spring Boot 的理由：
>
> 1. **企业级成熟度**：Spring 生态有20+年历史，文档齐全、社区活跃、遇到问题容易找到解决方案
> 2. **安全性内置**：Spring Security 提供完整的认证授权框架，JWT集成开箱即用
> 3. **WebSocket原生支持**：`@ServerEndpoint` 或 `WebSocketHandler` 配置简单
> 4. **ORM生态丰富**：MyBatis-Plus、JPA、JDBC Template 可自由组合
> 5. **团队协作友好**：Java 类型系统强，IDE 智能提示完善，多人协作时代码可读性高
> 6. **面试加分项**：Java/Spring Boot 是国内企业最主流的后端技术栈

---

## 三、认证安全类（JWT + RBAC）

### Q6: 什么是 JWT？为什么不用传统的 Session 认证？

**回答模板：**

> **JWT (JSON Web Token)** 是一种**无状态的认证令牌**，由三部分组成：
>
> ```
> Header.Payload.Signature
> │         │          │
> │         │          └── HMAC-SHA256 签名（防篡改）
> │         └── Payload: { sub:"zhangsan", iat:xxx, exp:xxx }
> └── Header: { alg:"HS256", typ:"JWT" }
> ```
>
> **Session vs JWT 对比：**
>
> | 特性 | Session | JWT |
> |------|---------|-----|
> | 存储位置 | 服务器内存/Redis | 客户端（Token） |
> | 跨域支持 | ❌ Cookie 同源限制 | ✅ Header 任意携带 |
> | 服务器压力 | 存储所有会话 | 无状态，零存储 |
> | 集群部署 | 需要 Session 共享 | 天然支持多实例 |
> | 移动端适配 | 不方便 | Token 直接传给 App |
>
> 本项目是**前后端分离架构**（前端8081端口，后端8080端口），存在跨域问题，Cookie 方案不适用，所以选择 JWT。

### Q7: JWT 如果被盗用了怎么办？安全性如何保障？

**回答模板：**

> 这是很好的问题！本项目的多重安全保障措施：
>
> 1. **双 Token 机制**：
>    - `accessToken`：有效期短（15分钟），即使泄露影响范围小
>    - `refreshToken`：有效期长，仅用于刷新 accessToken
>
> 2. **HTTPS 传输加密**（生产环境必须开启）：防止中间人截获 Token
>
> 3. **密码 BCrypt 加密存储**：数据库中不存明文密码
>
> 4. **Token 黑名单机制**（可扩展）：用户修改密码时将旧 Token 加入黑名单
>
> 5. **敏感操作二次验证**：删除好友时弹出确认框，转账类操作可加短信验证
>
> 6. **CORS 白名单限制**：只允许指定域名的前端访问 API

### Q8: 什么是 RBAC 权限模型？你的系统是怎么实现的？

**回答模板：**

> **RBAC = Role-Based Access Control（基于角色的访问控制）**
>
> 核心思想：**不给用户直接分配权限，而是通过角色间接分配**
>
> ```
> 用户(zhangsan) → 角色(学生) → 权限(发布物品、搜索、聊天)
> 用户(admin)   → 角色(管理员) → 权限(全部 + 用户管理+审核+统计)
> ```
>
> **本系统的三层控制：**
>
> **第一层：后端 Spring Security（接口级）**
> ```java
> // 管理员接口需要特定角色
> .antMatchers("/api/admin/**")
>     .hasAnyAuthority("ROLE_ADMIN", "ROLE_MANAGER")
>
> // 其他接口只需登录
> .anyRequest().authenticated()
> ```
>
> **第二层：前端路由守卫（页面级）**
> ```typescript
> router.beforeEach((to) => {
>   if (to.meta.roles && !hasRole(to.meta.roles)) {
>     return next('/login')  // 无权访问则跳转登录页
>   }
> })
> ```
>
> **第三层：数据库角色表设计**
> ```sql
> -- 三张表实现灵活的多对多关系
> users (id, username, ...)
> roles (id, code, name)        -- ROLE_STUDENT / ROLE_MANAGER / ROLE_ADMIN
> user_roles (user_id, role_id) -- 用户-角色关联
> ```
>
> **三种角色的权限矩阵：**
>
> | 功能 | 学生 | 负责人 | 管理员 |
> |------|:----:|:------:|:------:|
> | 发布/搜索物品 | ✅ | ✅ | ✅ |
> | 聊天/好友 | ✅ | ✅ | ✅ |
> | 物品审核 | ❌ | ✅ | ✅ |
> | 用户管理 | ❌ | ❌ | ✅ |
> | 地图点位管理 | ❌ | ❌ | ✅ |

---

## 四、核心功能实现思路类

### Q9: 好友添加功能的完整实现思路是什么？

**回答模板：**

> 好友功能的实现分为**三个层次**：数据库设计 → 后端API → 前端交互。
>
> **① 数据库设计（friendship 表）：**
> ```sql
> CREATE TABLE friendship (
>   id BIGINT PRIMARY KEY AUTO_INCREMENT,
>   requester_id BIGINT,     -- 发起请求的人
>   addressee_id BIGINT,     -- 接收请求的人
>   status VARCHAR(32),      -- PENDING / ACCEPTED / REJECTED
>   requester_alias VARCHAR, -- 发起方对对方的备注名
>   addressee_alias VARCHAR, -- 接收方对对方的备注名
>   created_at TIMESTAMP,
>   updated_at TIMESTAMP
> );
> ```
> 设计要点：**双向查找**——查询好友时要同时查 `requester_id=我 AND addressee_id=对方` 或者反过来。
>
> **② 后端 API 设计（5个接口）：**
>
> | 方法 | 路径 | 功能 |
> |------|------|------|
> | POST | `/api/friend/add` | 发送好友请求 |
> | GET | `/api/friend/list` | 获取我的好友列表 |
> | POST | `/api/friend/alias` | 修改好友备注名 |
> | DELETE | `/api/friend/{friendId}` | 删除好友 |
> | PUT | `/api/friend/{requestId}/respond` | 接受/拒绝请求 |
>
> 核心逻辑（以添加好友为例）：
> ```java
> public void sendRequest(Long fromId, Long toId) {
>   // 1. 不能自己加自己
>   if (fromId.equals(toId)) throw new Exception("不能添加自己");
>   
>   // 2. 检查是否已经是好友
>   Friendship existing = findFriendship(fromId, toId);
>   if (existing != null && "ACCEPTED".equals(existing.getStatus()))
>       throw new ConflictException("已经是好友");
>   
>   // 3. 检查是否已有待处理的请求（避免重复发送）
>   if (existing != null && "PENDING".equals(existing.getStatus()))
>       throw new ConflictException("已发送过请求");
>   
>   // 4. 创建新的好友请求记录
>   Friendship req = new Friendship();
>   req.setRequesterId(fromId);
>   req.setAddresseeId(toId);
>   req.setStatus("PENDING");  // 待对方接受
>   mapper.insert(req);
>   
>   // 5. 给对方发一条站内消息通知
>   messageService.send(toId, "好友请求", "xxx想添加你为好友", "FRIEND_REQUEST");
> }
> ```
>
> **③ 前端交互设计：**
>
> - **发起入口**：在物品列表页（Query.vue）点击发布者头像 → 弹出 Popover → 点击"添加好友"
> - **好友列表页**：个人中心(Profile.vue)"我的好友"Tab → 展示好友卡片
> - **每个好友卡片包含**：
>   - 头像 + 昵称/备注名 + 院系
>   - 💬 聊天按钮（蓝色）→ 打开 WebSocket 聊天窗口
>   - ✏️ 编辑备注按钮 → 弹出 Dialog 修改备注
>   - 🗑️ 删除按钮（红色）→ 确认框 → 调用 DELETE API → 从列表移除
>
> **④ 状态流转图：**
> ```
> PENDING(待处理) ──→ ACCEPTED(已是好友) ──→ DELETED(已删除)
>     │                    │
>     ↓                    ↓
>  REJECTED(被拒绝)    正常使用(聊天/备注)
> ```

### Q10: 图片上传和头像裁剪是怎么实现的？

**回答模板：**

> 这是一个**纯前端 Canvas 手写裁剪引擎**，没有依赖任何第三方库（如 cropperjs）。
>
> **为什么手写而不是用现成库？**
> - 减少约 50KB 的依赖包体积
> - 完全可控的定制化能力
> - 展示对 Canvas API 的掌握程度
>
> **完整流程：**
>
> ```
> 用户点击头像 → 触发 <input type="file"> 选择图片
>       ↓
> FileReader.readAsDataURL() 将图片转为 Base64 显示在裁剪区
>       ↓
> 用户在裁剪区操作：
>   ├─ 鼠标拖拽 → mousedown/mousemove/mouseup 移动图片位置
>   ├─ 滚轮缩放 → wheel event 调整图片大小（范围 0.02x ~ 50x）
>   ├─ 滑块缩放 → range input 微调缩放比例
>   └─ 圆形视口遮罩 → CSS box-shadow 技巧实现暗角效果
>       ↓
> 用户点击"确定"按钮
>       ↓
> WYSIWYG 裁剪算法（核心！）：
>   1. getBoundingClientRect() 获取裁剪区和图片的屏幕坐标
>   2. 计算自然尺寸 vs 显示尺寸的比例 (scaleX, scaleY)
>   3. 将圆形视口坐标映射到原图坐标系
>   4. canvas.drawImage(img, sx, sy, sWidth, sHeight, 0,0,400,400)
>   5. canvas.toBlob('image/jpeg', 0.95) 导出高清 JPEG
>       ↓
> FormData POST /api/profile/avatar → 后端保存到 uploads/ 目录
>       ↓
> 更新数据库 users.avatar 字段 → Vuex 刷新 → 页面头像更新
> ```
>
> **关键技术点详解：**
>
> **1. 圆形遮罩的实现（CSS技巧）：**
> ```css
> .viewport-mask {
>   width: 200px; height: 200px;
>   border-radius: 50%;
>   /* 核心：box-shadow 无限扩展实现"暗角" */
>   box-shadow: 0 0 0 9999px rgba(0,0,0,0.5);
>   pointer-events: none;  /* 让事件穿透到下层图片 */
> }
> ```
>
> **2. WYSIWYG 坐标映射算法：**
> ```typescript
> const confirmCrop = () => {
>   // 裁剪区的屏幕位置
>   const areaRect = areaEl.getBoundingClientRect()
>   // 圆形视口屏幕坐标（居中于300x300区域）
>   const vLeft = areaRect.left + areaRect.width/2 - 100  // 100 = 200/2
>   const vTop  = areaRect.top  + areaRect.height/2 - 100
>   
>   // 图片的屏幕位置
>   const imgRect = imgEl.getBoundingClientRect()
>   
>   // 自然尺寸与显示尺寸的比例
>   const scaleX = imgEl.naturalWidth  / imgRect.width
>   const scaleY = imgEl.naturalHeight / imgRect.height
>   
>   // 视口坐标 → 原图像素坐标
>   const srcX = (vLeft - imgRect.left) * scaleX
>   const srcY = (vTop  - imgRect.top)  * scaleY
>   const srcW = 200 * scaleX   // 视口200px映射到原图
>   const srcH = 200 * scaleY
>   
>   // Canvas 导出 400x400 高清图（Retina 2x）
>   ctx.drawImage(imgEl, srcX, srcY, srcW, srcH, 0, 0, 400, 400)
> }
> ```
>
> **3. 后端接收处理：**
> ```java
> @PostMapping("/profile/avatar")
> public ResponseEntity<?> uploadAvatar(@RequestParam MultipartFile file) {
>   String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
>   Path path = Paths.get(uploadDir, filename);  // uploads/ 目录
>   Files.createDirectories(path.getParent());
>   Files.copy(file.getInputStream(), path);     // 保存文件
>   
>   String url = "/static/" + filename;         // 构建访问URL
>   user.setAvatar(url);                         // 更新数据库
>   userRepository.save(user);
>   return ResponseEntity.ok(Map.of("url", url));
> }
> ```

### Q11: 物品发布的完整流程是怎样的？

**回答模板：**

> 以**发布拾物（Found Item）**为例：
>
> **前端（FoundPublish.vue）：**
> ```
> 用户填写表单:
>   ├── 标题 (title)           ← 必填，el-input
>   ├── 分类 (category)        ← el-select: 证件/电子设备/文具/生活用品/其他
>   ├── 描述 (description)     ← el-input textarea
>   ├── 地点 (location)        ← el-input + 可选地图选点
>   ├── 图片 (images)          ← UploadImage 组件（最多5张）
>   │   ├── 选择本地图片
>   │   ├── 前端预览（Base64）
>   │   └── 大于5MB自动Canvas压缩到1024x1024
>   └── 联系方式 (contact)      ← el-input
>       
> 点击"发布"按钮
>       ↓
> 表单校验（Element Plus rules）
>       ↓
> POST /api/found { title, category, description, location, images[], contact }
>       ↓
> 成功提示 → 跳转到物品列表页
> ```
>
> **后端（FoundController + FoundItem entity）：**
> ```java
> @PostMapping
> public ResponseEntity<FoundItem> create(
>     @AuthenticationPrincipal String username,
>     @RequestBody FoundItem item) {
>   
>   User user = userService.getByUsername(username);
>   item.setUserId(user.getId());
>   item.setStatus("PENDING");        // 新发布需审核
>   item.setAuditStatus(0);           // 未审核
>   item.setClaimStatus(0);           // 未被认领
>   item.setCreatedAt(LocalDateTime.now());
>   
>   // 保存到数据库
>   FoundItem saved = foundItemMapper.insert(item);
>   
>   return ResponseEntity.status(201).body(saved);
> }
> ```
>
> **物品生命周期：**
> ```
> PENDING(待审核) 
>     ↓ 管理员审核通过
> APPROVED(待认领) 
>     ↓ 有人申请认领
> CLAIM_AUDITING(认领审核中)
>     ↓ 管理员确认认领
> CLAIMED(已认领/已归还)
> ```

### Q12: 搜索查询功能怎么实现的？

**回答模板：**

> 搜索功能支持**多维度组合查询**，在后端使用 MyBatis-Plus 的 QueryWrapper 动态拼接 SQL：
>
> **前端参数：**
> ```typescript
> // Query.vue 中收集的查询条件
> params: {
>   page: 0,              // 分页
>   size: 10,
>   category: 'card',     // 物品类别（可选）
>   keyword: '身份证',    // 关键词模糊搜索（可选）
>   location: '北门',     // 地点模糊搜索（可选）
>   status: 'APPROVED'    // 仅显示已审核（可选）
> }
> ```
>
> **后端动态SQL构建：**
> ```java
> public Page<FoundItem> query(ItemQueryDTO dto) {
>   QueryWrapper<FoundItem> wrapper = new QueryWrapper<>();
>   
>   // 动态拼接条件（只拼接非空的字段）
>   if (StringUtils.hasText(dto.getCategory()))
>       wrapper.eq("category", dto.getCategory());           // 精确匹配
>   
>   if (StringUtils.hasText(dto.getKeyword()))
>       wrapper.like("title", dto.getKeyword())               // 标题模糊搜索
>              .or().like("description", dto.getKeyword());   // 或描述模糊搜索
>   
>   if (StringUtils.hasText(dto.getLocation()))
>       wrapper.like("location", dto.getLocation());          // 地点模糊搜索
>   
>   if (dto.getStatus() != null)
>       wrapper.eq("status", dto.getStatus());                // 状态精确匹配
>   
>   // 智能匹配度排序（如果有关键词，按相关度降序）
>   wrapper.orderByDesc("created_at");
>   
>   return foundItemMapper.selectPage(new Page<>(dto.getPage(), dto.getSize()), wrapper);
> }
> ```
>
> **智能匹配度展示：**
> - 前端展示每条结果的 `matchScore`（0~1之间的小数）
> - 转换为百分比显示：`(matchScore * 100).toFixed(0) + '%'`
> - 匹配度越高，说明越符合用户的搜索意图

---

## 五、WebSocket 实时通信类

### Q13: 为什么用 WebSocket 而不是 HTTP 轮询来实现聊天功能？（⭐ 高频题）

**回答模板：**

> 这是最关键的设计决策之一。让我详细对比两种方案：
>
> **HTTP 轮询模式（Polling）：**
> ```
> 客户端                    服务器
>   │                          │
>   │ ── GET /messages ──────> │  返回: [] (无新消息)
>   │ <─────────────────────── │
>   │                          │
>   │ ── GET /messages ──────> │  返回: [] (无新消息)
>   │ <────────────────────── │
>   │                          │
>   │ ── GET /messages ──────> │  返回: ["你好!"] (终于有新消息了)
>   │ <────────────────────── │
>   │                          │
>   │ ── GET /messages ──────> │  返回: [] (又是空)
>   │ <────────────────────── │
>   │ ... 每2秒一次，大量无效请求 ...
> ```
>
> **问题分析：**
> - **延迟高**：最坏情况要等一个轮询周期才能收到消息（最多2秒延迟）
> - **浪费带宽**：假设 100 个在线用户，每秒产生 50 次 HTTP 请求，其中 90% 返回空结果
> - **服务器压力大**：每次请求都要解析 HTTP 头、验证 Token、查询数据库
>
> **WebSocket 全双工模式：**
> ```
> 客户端                    服务器
>   │                          │
>   │ ── GET /ws/chat ──────> │  HTTP 升级握手
>   │ <── 101 Switching ──── │  升级为 WebSocket 连接
>   │══════ TCP 长连接 ══════│
>   │                          │
>   │ <── "你好!" (主动推送)──│  有新消息立即送达！毫秒级
>   │                          │
>   │ ── "在吗？" ──────────>│  客户端随时发
>   │ <── "在的！" (立即到达)─│
>   │                          │
>   │ ... 连接保持，只有有数据才传输 ...
> ```
>
> **量化对比：**
>
> | 维度 | HTTP轮询(2s间隔) | WebSocket |
> |------|-------------------|-----------|
> | **消息延迟** | 0 ~ 2000ms | **0 ~ 50ms** |
> | **每用户请求数/分钟** | **30次** | **1次**(握手) |
> | **无效请求占比** | **~90%** | **0%** |
> | **服务器CPU占用** | 高（频繁处理请求） | 低（事件驱动） |
> | **用户体验** | 有明显卡顿感 | **类似微信的流畅体验** |
>
> **本项目的实际实现：**
> ```java
> @Configuration
> @EnableWebSocket
> public class WebSocketConfig implements WebSocketConfigurer {
>   @Override
>   public void registerWebSocketHandlers(Registry registry) {
>     registry.addHandler(new ChatWebSocketHandler(), "/ws/chat")
>         .setAllowedOrigins("*");  // 允许前端跨域连接
>   }
> }
> ```
>
> ```java
> @Component
> public class ChatWebSocketHandler extends TextWebSocketHandler {
>   // 在线用户 Session 映射表
>   private Map<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();
>   
>   // 收到消息 → 保存数据库 → 推送给接收方
>   protected void handleTextMessage(Session session, TextMessage msg) {
>     ChatMessage chat = JSON.parse(msg.getPayload());
>     
>     // 1. 保存到数据库
>     chatMessageMapper.insert(chat);
>     
>     // 2. 查找接收方的在线 Session
>     WebSocketSession receiver = onlineUsers.get(chat.getReceiverId());
>     if (receiver != null && receiver.isOpen()) {
>       receiver.sendMessage(msg);  // 主动推送到客户端！
>     } else {
>       // 对方不在线 → 发送站内信通知
>       messageService.send(chat.getReceiverId(), "新消息", "...", "CHAT");
>     }
>   }
> }
> ```
>
> **总结一句话：** 聊天场景的核心诉求是**低延迟 + 即时性**，WebSocket 的全双工特性天然满足这一需求，而 HTTP 轮询本质上是用"不断询问"模拟"即时通知"，效率和体验都差很多。

### Q14: WebSocket 断线怎么办？如何保证消息不丢失？

**回答模板：**

> 好问题！本项目的**混合策略**来解决断线问题：
>
> **策略1：前端定时拉取兜底（已实现）**
> ```typescript
> // Profile.vue 中，聊天窗口打开时每2秒刷新历史记录
> setInterval(async () => {
>   if (replyVisible.value && replyTarget.value) {
>     await loadHistory(true)  // 加载最新消息
>   }
> }, 2000)
> ```
> 即使 WebSocket 断了，轮询也能保证消息最终到达。
>
> **策略2：消息持久化到数据库**
> 所有聊天消息先存入 `chat_message` 表，WebSocket 只是**推送通知**，不是唯一传输通道。
>
> **策略3：心跳检测（可扩展）**
> ```java
> // 服务端定期发送 ping 帧
> // 客户端收到后回复 pong 帧
> // 如果超时未收到 pong → 判定断线 → 清除 session
> ```
>
> **策略4：自动重连（可扩展）**
> ```typescript
> // 前端监听 onclose/onerror 事件，指数退避重连
> let retryCount = 0;
> ws.onclose = () => {
>   setTimeout(() => connect(), Math.min(1000 * 2^retryCount, 30000));
>   retryCount++;
> };
> ```

---

## 六、数据库设计类

### Q15: 你的数据库有多少张表？它们之间的关系是怎样的？

**回答模板：**

> **共 11 张表**，分为 5 大模块：
>
> **① 用户权限模块（3张表）：**
> ```
> users ──N:N── user_roles ──N:N── roles
>   │                              │
>   │ id, username, password_hash  │ id, code, name
>   │ avatar, nickname             │ (STUDENT/MANAGER/ADMIN)
>   │ department, phone
> ```
>
> **② 物品模块（2张表）：**
> ```
> lost_item (失物)     found_item (拾物)
>   │                      │
>   ├── user_id(FK→users)  ├── user_id(FK→users)
>   ├── point_id(FK→map_point)
>   ├── claim_user_id(FK→users)  -- 认领人
>   └── status: PENDING/APPROVED/CLAIMED
> ```
>
> **③ 社交模块（3张表）：**
> ```
> friendship (好友关系)     chat_message (聊天消息)    message (站内信)
>   │                           │                        │
>   ├── requester_id(FK)       ├── sender_id(FK)        ├── user_id(FK)
>   ├── addressee_id(FK)      ├── receiver_id(FK)       └── type: FRIEND_REQUEST/
>   └── status: PENDING/ACCEPTED                       MATCH/CLAIM/AUDIT
> ```
>
> **④ 内容模块（2张表）：**
> ```
> map_point (地图点位)       notice (公告)
>   │                          │
>   ├── name, latitude/lng   ├── title, content
>   └── description          └── image_url
> ```
>
> **⑤ 配置模块（1张表）：**
> ```
> sys_config (系统配置)
>   ├── cfg_key (PRIMARY KEY)
>   └── cfg_value (TEXT)
> ```
>
> **ER 关系总览：**
> ```
> users ──1:N──> lost_item
> users ──1:N──> found_item
> users ──1:N──> message
> users ──1:N──> chat_message (sender)
> users ──1:N──> chat_message (receiver)
> users ──N:N──> friendship
> users ──N:N──> roles (via user_roles)
> map_point ──1:N──> lost_item
> map_point ──1:N──> found_item
> ```

### Q16: 为什么 lost_item 和 found_item 是两张表而不是一张？

**回答模板：**

> 虽然两张表的字段几乎一样，但分开设计的原因：
>
> 1. **业务语义清晰**：失物和拾物是两个不同的业务概念，分别对应不同的用户行为
> 2. **独立统计方便**："今日新增失物 N 条"、"今日新增拾物 M 条" 可以直接 COUNT 各自的表
> 3. **权限隔离**：未来可能对不同类型的物品设置不同的审核规则或展示策略
> 4. **索引优化**：可以为不同表建立不同的索引策略
> 5. **扩展灵活性**：未来失物表可能增加"丢失时间"字段，拾物表增加"拾获地点详情"，互不影响
>
> 如果合并成一张表用 type 字段区分，虽然减少了表数量，但牺牲了上述优势。对于当前的业务规模，**表数量少不是瓶颈，清晰的业务建模更重要**。

---

## 七、难点与亮点类

### Q17: 项目中你觉得最有挑战性的部分是什么？你是怎么解决的？

**回答模板（任选1-2个）：**
>
> **挑战1：Canvas 图片裁剪器的 WYSIWYG 实现**
>
> 最大的难点在于**所见即所得**——用户在界面上看到的裁剪区域，必须精确映射到原始图片的像素坐标。
>
> 解决过程：
> - 尝试1：直接用 CSS transform 缩放图片 → 失败，坐标计算不准
> - 尝试2：用 cropperjs 库 → 能用但不想依赖第三方
> - 最终方案：深入研究 `getBoundingClientRect()` API 和 Canvas `drawImage()` 九参数签名，实现了精确的屏幕坐标→自然像素映射
>
> **挑战2：好友关系的双向查询**
>
> 好友表中 A 加 B 时，requester_id=A, addressee_id=B。但 B 查看好友列表时，需要同时查出自己是 requester 还是 addressee 的情况。
>
> 解决方案：使用 MyBatis-Plus 的 `or()` 条件构造器：
> ```java
> new QueryWrapper<Friendship>()
>     .eq("requester_id", myId).eq("addressee_id", friendId)
>     .or()
>     .eq("requester_id", friendId).eq("addressee_id", myId)
> ```
>
> **挑战3：WebSocket 在线状态管理**
>
> 需要维护 userId → WebSocketSession 的映射，但要处理：用户多标签页登录、异常断线、服务端重启等情况。
>
> 解决方案：使用 ConcurrentHashMap 保证线程安全 + 定时清理过期 session。

### Q18: 如果让你重新做这个项目，你会改进什么？

**回答模板：**

> 1. **引入 Redis**：缓存热点数据（热门物品、在线用户列表）、分布式 Session
> 2. **消息队列**：异步处理通知发送、图片压缩等耗时操作
> 3. **全文检索**：用 Elasticsearch 替代 SQL LIKE，提升搜索性能
> 4. **单元测试**：补充 Service 层和 Controller 层的单元测试，目前覆盖率偏低
> 5. **API 文档**：接入 Swagger/OpenAPI 3.0，自动生成接口文档
> 6. **日志规范**：引入 SLF4J + Logback，统一日志格式，便于排查线上问题
> 7. **Docker化部署**：编写 Dockerfile 和 docker-compose.yml，一键部署整套环境

---

## 八、优化与展望类

### Q19: 你的项目还有什么不足？未来有什么规划？

**回答模板：**

> **当前不足：**
> - 单机部署，不支持水平扩展（MySQL 单点、文件存储在本地磁盘）
> - 没有引入缓存机制，高并发下数据库压力较大
> - 测试覆盖不够充分，主要靠手动测试
> - 移动端适配有限，主要针对 PC 端浏览器
>
> **未来规划（短期 3个月）：**
> - 引入 Redis 缓存 + Redisson 分布式锁
> - 接入阿里云 OSS 存储图片，替代本地 uploads 目录
> - 补充单元测试和集成测试
> - 编写 Swagger API 文档
>
> **未来规划（长期 6-12个月）：**
> - 微服务拆分：用户服务、物品服务、消息服务独立部署
> - 小程序/移动端适配
> - Elasticsearch 全文搜索引擎
> - 消息队列（RabbitMQ/Kafka）异步解耦
> - 推送通知（短信/邮件/微信模板消息）

---

## 附录：答辩速记卡

### 必背关键词

| 类别 | 关键词 |
|------|--------|
| 前端 | Vue 3, TypeScript, Composition API, Element Plus, Vuex, Axios拦截器, Canvas API |
| 后端 | Spring Boot 2.7, MyBatis-Plus, JPA/Hibernate, Spring Security, RESTful API |
| 认证 | JWT (双Token), RBAC, BCrypt密码加密, CORS跨域 |
| 实时通信 | WebSocket, 全双工, Server-Sent Events对比, 心跳重连 |
| 数据库 | MySQL 8, InnoDB事务, 外键约束, 索引优化 |
| 设计模式 | MVC分层, Controller-Service-Mapper, 策略模式, 工厂模式 |
| 部署 | Docker Compose, Nginx反向代理, CI/CD |

### 自我评价话术

> 这个项目让我完整实践了一个**从零到一的 Web 应用开发全流程**。最大的收获是对**前后端分离架构**的深入理解，以及在实际开发中解决**跨域、认证、实时通信**等技术问题的能力。特别是手写 Canvas 裁剪器和 WebSocket 聊天功能的实现，让我对底层原理有了更深的认识。
