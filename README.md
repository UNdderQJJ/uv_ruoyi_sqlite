# 项目介绍
基于[若依vue springboot3版本](https://gitee.com/y_project/RuoYi-Vue) 实现的一套无mysql，无redis的项目。

目的在于支持那些极度轻量级的项目，支持快速启动，减少开发成本和部署成本。

希望通过一个jar服务，实现一个完整的项目。但因为引入了sqlite和caffeine, 本项目只能当做一个轻量级单体项目来使用，不支持微服务架构。

本项目将与若依版本保持一致。

# 技术栈
- springboot 3.3.5
- java 17
- sqlite 3.48.0
- Caffeine 
