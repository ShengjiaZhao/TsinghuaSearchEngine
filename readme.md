# Heritrix抓取数据
## 主要工作
* 设置爬虫的seed为```http://news.tsinghua.edu.cn/```和```http://info.tsinghua.edu.cn/```
* 设置搜索的范围，具体为拒绝所有链接，除非这些链接属于tsinghua.edu.cn域，或者从tsinghua.edu.cn域直接指向．但是不爬取过长，以某些特定格式结尾的链接，如mpeg，或者不能正确解析的链接．
* 最后设置存储的方式为镜像，并且只存储支持的格式(htm, html, pdf)

# Lucene构建搜索引擎框架
## 主要工作
* 根据链接结构计算每个网页的PageRank
* 遍历所有文档，使用IKAnalyzer进行分词，构建索引，每个文档的权重(Boost)由其PageRank决定
* 提供单词搜索功能，返回相似度和PageRank加权最高的文档集合

# Tomcat构建搜索前端
## 主要工作
* 搭建了MyEclipse+Tomcat7基本框架
* 处理了HTTP Get请求，返回相应搜索结果

## 运行截图
![alt text](https://github.com/ShengjiaZhao/TsinghuaSearchEngine/blob/master/graphs/query_example.png)

