# tree-new-bee
# 树新蜂

# 通信接口协议  
## 目前支持的连接协议
- TCP，缺省端口32167，消息采用分隔模式，消息用`\r\n`分隔，服务器初始不会处理消息，直至收到`\r\n`     
- Web Socket，缺省端口32168

## 消息格式  

### 消息采用JSON格式封装
- 名称均使用小写驼峰格式（camelCase）
- 编码为`UTF-8`

例如：   
```json
{ "myId" : "123456" }   
```
建立连接之后，服务器端会立即发送一个JSON消息确认，该消息仅包含有一个随机生成的id字段，该id与此次使用的tcp连接一一对应。  
例如：   
```json
{"id":"b51cbc1d-a694-479b-ac94-68b4c52d1b99"}
```
  
### 客户端 -> 服务器端 发送消息的JSON格式
  
若包含 nickname 字段，则服务器会更新昵称，并将更新后的昵称列表广播给其他连接。   
例如：
```json 
{ "nickname" : "草原猎豹" }   
```
若包含 message 字段，则服务器会将该消息自动广播给其他连接。  
例如：
```json
{ "message" : "大家好，我是草原猎豹，来自厦门，有两个娃。" }
```

### 服务器端 -> 客户端 发送消息的JSON格式

若包含 nicknames 字段，则表示在线用户需要更新。   
例如：
```json
{"nicknames":["草原猎豹", "面筋锅"]}
```
若包含 message 字段，则表示有用户发言。系统转发消息时，会自动添加id，nickname和time三个字段。  
例如：
```json
{
  "message":"大家好",
  "nickname":"草原猎豹",
  "id":"b51cbc1d-a694-479b-ac94-68b4c52d1b99",
  "time":"2022-03-01 10:29:06 +0800"
}
```
---
以上便是封装协议的核心内容，服务器端仅做这些基础校验，以下是扩展功能，**注意服务器并不能保证所有客户端都遵照以下格式发送，客户端应该考虑并处理不遵守以下格式的消息**。   
message除了可用String字符串类型以外，亦可使用Json Array和Json Object。    
Json Array 可用来拼接不同的消息为一个消息，例如：    
```json
{
  "message" : [
    {"message":"这是写在图片前面的文字"}, 
    {"message":"http://i.17173cdn.com/2fhnvk/YWxqaGBf/cms3/EJUEJhbnjBEDkEz.png"},
    {"message":"这是写在图片后面的文字"}
  ]
}
```
Json Object 可用来提供更为复杂详尽的格式封装消息，需使用type字段以区分不同类型图片：   
type：表示具体的消息类型，包含有文本，图像，超链接，视频，音频，聊天记录，以下针对不同消息展开     
     
文本类型消息：默认消息类型，不指定type，或设置为"text"|"txt"|0，就被默认为文本类型消息，以下是一个典型的文本消息例子：    
```json
{
  "message":{
    "type":0,
    "content":"hello"
  }
}
```
     
图像类型消息：指定type为"image"|"img"|1，需指定图像在互联网上的地址，或提供Base64编码，以下是一个典型的图像消息例子：
```json
{
  "message":{
    "type":"img",
    "url":"http://i.17173cdn.com/2fhnvk/YWxqaGBf/cms3/EJUEJhbnjBEDkEz.png"
  }
}
```
```json
{
  "message":{
    "type":"img",
    "base64":"M2Q4YmUxMTEtYWRkZi00NzBlLTgyZDgtN2MwNjgzOGY2NGFlOTQ3NDYyMWEtZDM4ZS00YWVhLTkzOTYtY2ZjMzZiMzFhNmZmOGJmOGI2OTYtMzkxZi00OTJiLWEyMTQtMjgwN2RjOGI0MTBmZWUwMGNkNTktY2ZiZS00MTMxLTgzODctNDRjMjFkYmZmNGM4Njg1NDc3OGItNzNlMC00ZWM4LTgxNzAtNjY3NTgyMGY3YzVhZWQyMmNiZGItOTIwZi00NGUzLTlkMjAtOTkzZTI1MjUwMDU5ZjdkYjg2M2UtZTJmYS00Y2Y2LWIwNDYtNWQ2MGRiOWQyZjFiMzJhMzYxOWQtNDE0ZS00MmRiLTk3NDgtNmM4NTczYjMxZDIzNGRhOWU4NDAtNTBiMi00ZmE2LWE0M2ItZjU3MWFiNTI2NmQ2NTlmMTFmZjctYjg1NC00NmE1LWEzMWItYjk3MmEwZTYyNTdk"
  }
}
```
     
超链接类型消息：指定type为"hyperlink"|"link"|"url"|2，需指定地址，以下是一个典型的超链接消息例子：
```json
{
  "message":{
    "type":"link",
    "url":"http://www.google.com",
    "content": "谷歌网站"
  }
}
```
     
历史/聊天记录类型消息：指定type为"history"|3，消息体为JsonArray，以下是一个典型的历史消息例子：
```json
{
  "message":{
    "type":"history",
    "content": [{"message": "豹哥，你在哪里？"},{"message": {"type": 0,"content": "我在厦门"}}]
  }
}
```
     
视频类型消息：指定type为"video"|4，以下是一个典型的视频消息例子：
```json
{
  "message":{
    "type":"video",
    "url": "https://www.youtube.com/watch?v=niG3YMU6jFk"
  }
}
```

音频类型消息：指定type为"audio"|5，以下是一个典型的音频消息例子：
```json
{
  "message":{
    "type":"audio",
    "url": "https://m.qingting.fm/channels/4804/from/20220304210000/"
  }
}
```
---
若包含 color 字段，则表示该消息需要用特殊颜色（网页颜色，例如#FF00FF）显示。   
例如：
```json
{
  "message":"我是洋红色的草原猎豹",
  "color":"#FF00FF",
  "nickname":"草原猎豹",
  "id":"b51cbc1d-a694-479b-ac94-68b4c52d1b99",
  "time":"2022-03-01 10:30:22 +0800"
}
```
