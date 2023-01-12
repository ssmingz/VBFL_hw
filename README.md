# VBFL_hw

#### 使用说明

1. autoRun/collect_bug_info.py配置缺陷信息：修改文件中的路径groundtruth, test_commands和起始编号counter

2. autoRun/copy_mysql_bug.py配置缺陷源码：修改路径groundtruth, src_root, dest_root和起始编号counter

3. autoRun/ochiai.py收集SBFL结果：修改路径build_dest_dir, bugs_txt_dir和SCORE_BASE_ROOT

4. autoRun/autoRun.py插桩

5. values/collect_values.py运行测试收集值

6. values/match_method.py匹配图的方法名：修改路径graph_root和output_file

7. code/VBFL_hw/parse_values.py改range()把值按函数分文件夹：修改路径values_path和output_dir

8. code/VBFL_hw/src/fl/weka/IntraGenTree.java构建树：修改路径root_dir, mid_path和graph_map_path

   ```shell
   # 在VBFL_hw/下，先创建target文件夹
   # 编译
   javac -cp "lib/*" -sourcepath src -d target src/fl/weka/IntraGenTree.java
   # 运行
   java -cp "target:lib/*" fl.weka.IntraGenTree
   ```
   
9. values/check_rank.py获取排名(函数内）：修改路径groundtruth_path, trees_root（8.中root_dir）, values_root和src_file

10. code/VBFL_hw/find_all_covered_tests.py用于获取覆盖某文件某行的所有large/small test运行命令

11. code/VBFL_hw/compare_tests.py用于跟groundtruth比较是否有新增的覆盖的测试

groundtruth_path文件格式：

bugid,bug_type,src_file_path,buggy_line1:buggy_line2:...,test_suite,failing_test,test_command

比如：1,delete condition,sql/sql_table.cc,4167,SqlTableTest.*,SqlTableTest.PromoteFirstTimestampColumn2,merge_large_tests-t


1-9 均无输入参数

