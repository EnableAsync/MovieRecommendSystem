# 系统架构
![架构图](docs/架构图.svg)

# 使用说明
## 注册
![注册](docs/register.png)

## 登录
![](docs/login.png)

## 冷启动
![冷启动](docs/setup.png)

## 热门电影
![热门电影](docs/hot.png)

## 排行榜
![排行榜](docs/rank.png)

## 实时推荐
![实时推荐](docs/stream.png)

## 离线推荐
![离线推荐](docs/offline.png)

## 后端
- Spring
- MongoDB
在推荐系统中多为半结构化、非结构化数据，使用MongoDB便于存储
- Redis
- ElasticSearch

## 前端
使用了 Vue + Vuetify，详情见
[推荐系统前端](https://github.com/EnableAsync/recommender-frontend)