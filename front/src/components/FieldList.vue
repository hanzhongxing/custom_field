<template>
  <div class="field-list">
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 新建字段
      </el-button>
    </div>

    <el-table :data="fields" row-key="key" border v-loading="loading">
      <el-table-column prop="sortOrder" label="序号" width="70" align="center" />
      <el-table-column prop="name" label="字段名称" width="150" />
      <el-table-column prop="key" label="业务主键" width="150" />
      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTag(row.type)">{{ row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="expression" label="表达式" min-width="150" />
      <el-table-column label="当前值 / 计算结果" min-width="180">
        <template #default="{ row }">
          <template v-if="!row.expression">
            <el-input 
              v-model="env[row.key]" 
              @input="handleEnvInput" 
              placeholder="输入基础值" 
              size="small"
            />
          </template>
          <template v-else>
            <el-tag type="info" style="font-size:14px; font-weight:bold;">
              {{ env[row.key] !== undefined ? env[row.key] : '-' }}
            </el-tag>
          </template>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-switch
            v-model="row.status"
            active-value="ENABLED"
            inactive-value="DISABLED"
            @change="handleStatusChange(row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" align="center">
        <template #default="{ row, $index }">
          <el-button link type="primary" @click="moveUp($index)" :disabled="$index === 0">上移</el-button>
          <el-button link type="primary" @click="moveDown($index)" :disabled="$index === fields.length - 1">下移</el-button>
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <FieldEditDialog
      v-model:visible="dialogVisible"
      :field-data="currentField"
      :all-fields="fields"
      @saved="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getFields, saveField, deleteField, updateSort, evaluateAll } from '../api/field'
import { ElMessage, ElMessageBox } from 'element-plus'
import FieldEditDialog from './FieldEditDialog.vue'

const fields = ref([])
const env = ref({}) // Stores input and calculated values
const loading = ref(false)
const dialogVisible = ref(false)
const currentField = ref(null)

let debounceTimer = null
const handleEnvInput = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    runEvaluateAll()
  }, 500)
}

const runEvaluateAll = async () => {
  try {
    const formattedEnv = {}
    for (const k in env.value) {
      const val = env.value[k]
      formattedEnv[k] = isNaN(Number(val)) ? val : Number(val)
    }
    const result = await evaluateAll(formattedEnv)
    env.value = result
  } catch (e) {
    ElMessage.error('计算失败: ' + e.message)
  }
}

const fetchData = async () => {
  loading.value = true
  try {
    fields.value = await getFields()
    // Initialize env with empty strings for base fields if they are not already set
    fields.value.forEach(f => {
      if (!f.expression && env.value[f.key] === undefined) {
        env.value[f.key] = ''
      }
    })
    runEvaluateAll() // Initial calculation
  } catch (e) {
    ElMessage.error('获取列表失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

const getTypeTag = (type) => {
  const map = { NUMERIC: '', TEXT: 'success', DATE: 'warning' }
  return map[type] || 'info'
}

const openDialog = (row = null) => {
  currentField.value = row ? JSON.parse(JSON.stringify(row)) : null
  dialogVisible.value = true
}

const handleStatusChange = async (row) => {
  try {
    await saveField(row)
    ElMessage.success('状态更新成功')
  } catch (e) {
    ElMessage.error('状态更新失败: ' + e.message)
    row.status = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确认删除字段 ${row.name} 吗?`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteField(row.key)
      ElMessage.success('删除成功')
      fetchData()
    } catch (e) {
      ElMessage.error('删除失败: ' + e.message)
    }
  }).catch(() => {})
}

const moveUp = async (index) => {
  if (index === 0) return
  const temp = fields.value[index]
  fields.value[index] = fields.value[index - 1]
  fields.value[index - 1] = temp
  saveSort()
}

const moveDown = async (index) => {
  if (index === fields.value.length - 1) return
  const temp = fields.value[index]
  fields.value[index] = fields.value[index + 1]
  fields.value[index + 1] = temp
  saveSort()
}

const saveSort = async () => {
  const keys = fields.value.map(f => f.key)
  try {
    await updateSort(keys)
    // quietly success
  } catch (e) {
    ElMessage.error('保存排序失败: ' + e.message)
    fetchData()
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.toolbar {
  margin-bottom: 20px;
}
</style>
