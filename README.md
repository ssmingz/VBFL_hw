# VBFL_hw

#### 使用说明

1. autoRun.py插桩
2. 运行测试收集值
3. match_method.py匹配图的方法名：修改文件中的路径graph_root和output_file
4. parse_values.py改range()把值按函数分文件夹：修改路径values_path和output_dir
5. src/fl/weka/IntraGenTree.java构建树，修改路径root_dir, mid_path和graph_map_path
6. check_rank.py获取排名(函数内）：修改路径groundtruth_path, trees_root（5.中root_dir）, values_root和src_file

3-6 均无输入参数