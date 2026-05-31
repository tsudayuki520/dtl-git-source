# API 接口文档

## 通用说明

### Base URL

```
http://localhost:8080
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200 表示成功 |
| message | string | 响应消息 |
| data | object | 业务数据 |

---

## 轮播图模块

### 1. 获取轮播图列表

获取所有启用状态的轮播图，按排序序号升序排列。

**请求**

```
GET /api/banner/list
```

**参数**：无

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "imageUrl": "https://wxsportproject.obs.cn-north-4.myhuaweicloud.com/banner/1.jpg",
      "title": "轮播图1",
      "sortOrder": 1,
      "status": 1,
      "createTime": "2026-05-28T10:00:00",
      "updateTime": "2026-05-28T10:00:00"
    },
    {
      "id": 2,
      "imageUrl": "https://wxsportproject.obs.cn-north-4.myhuaweicloud.com/banner/2.jpg",
      "title": "轮播图2",
      "sortOrder": 2,
      "status": 1,
      "createTime": "2026-05-28T10:00:00",
      "updateTime": "2026-05-28T10:00:00"
    }
  ]
}
```

**微信小程序调用**

```javascript
wx.request({
  url: 'http://localhost:8080/api/banner/list',
  method: 'GET',
  success(res) {
    if (res.data.code === 200) {
      // res.data.data 为轮播图数组
    }
  }
})
```

---

### 2. 从 OBS 同步轮播图

从华为云 OBS 的 `banner/` 目录拉取所有图片 URL，写入数据库。

**请求**

```
POST /api/banner/sync
```

**参数**：无

**响应示例**

```json
{
  "code": 200,
  "message": "success",
  "data": "同步完成，共插入 9 条"
}
```

**注意**

- 调用前确保 OBS 桶中 `article/` 目录下有图片
- 重复调用会产生重复数据，建议先清空表：`DELETE FROM banner;`
