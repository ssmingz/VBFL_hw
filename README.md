# VBFL_hw

#### 使用说明

1. autoRun/collect_bug_info.py配置缺陷信息：修改文件中的路径groundtruth, test_commands和起始编号counter
2. autoRun/copy_mysql_bug.py配置缺陷源码：修改路径groundtruth, src_root, dest_root和起始编号counter
3. autoRun/ochiai.py收集SBFL结果：修改路径build_dest_dir, bugs_txt_dir和SCORE_BASE_ROOT
4. autoRun/autoRun.py插桩
5. values/collect_values.sh运行测试收集值
6. values/match_method.py匹配图的方法名：修改路径graph_root和output_file
7. code/VBFL_hw/parse_values.py改range()把值按函数分文件夹：修改路径values_path和output_dir
8. code/VBFL_hw/src/fl/weka/IntraGenTree.java构建树：修改路径root_dir, mid_path和graph_map_path
9. values/check_rank.py获取排名(函数内）：修改路径groundtruth_path, trees_root（8.中root_dir）, values_root和src_file

groundtruth_path文件格式：

bugid,bug_type,src_file_path,buggy_line1:buggy_line2:...,test_suite,failing_test,test_command

比如：1,delete condition,sql/sql_table.cc,4167,SqlTableTest.*,SqlTableTest.PromoteFirstTimestampColumn2,merge_large_tests-t


1-9 均无输入参数