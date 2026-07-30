<template>
  <el-dialog
    :title="isEdit ? '编辑字段' : '新建字段'"
    :model-value="visible"
    @update:model-value="$emit('update:visible', $event)"
    width="800px"
    destroy-on-close
  >
    <div class="dialog-layout">
      <!-- Left side: Form -->
      <div class="form-container">
        <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
          <el-form-item label="业务主键" prop="key">
            <el-input v-model="form.key" :disabled="isEdit" placeholder="如: field_a" />
          </el-form-item>
          <el-form-item label="字段名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入名称" />
          </el-form-item>
          <el-form-item label="类型" prop="type">
            <el-select v-model="form.type" placeholder="请选择类型" class="w-full">
              <el-option label="数值 (NUMERIC)" value="NUMERIC" />
              <el-option label="文本 (TEXT)" value="TEXT" />
              <el-option label="日期 (DATE)" value="DATE" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio label="ENABLED">启用</el-radio>
              <el-radio label="DISABLED">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="2" />
          </el-form-item>
          
          <el-divider>表达式定义</el-divider>
          
          <el-form-item label="表达式" prop="expression">
            <el-input 
              v-model="form.expression" 
              type="textarea" 
              :rows="3" 
              placeholder="例如: field_a * 1.2"
              @blur="parseVariables"
            />
          </el-form-item>
          
          <div class="simulation-area" v-if="variables.length > 0">
            <div class="sim-title">模拟执行测试 (Dry-Run)</div>
            <div class="sim-vars">
              <div v-for="v in variables" :key="v" class="sim-var-item">
                <span>{{ v }}: </span>
                <el-input v-model="env[v]" size="small" style="width: 120px" />
              </div>
            </div>
            <div class="sim-action">
              <el-button type="success" size="small" @click="runSimulation" :loading="simulating">测试执行</el-button>
              <span class="sim-result" v-if="simResult !== null">
                结果: <el-tag :type="simError ? 'danger' : 'success'">{{ simResult }}</el-tag>
              </span>
            </div>
          </div>
        </el-form>
      </div>
      
      <!-- Right side: Variable Reference -->
      <div class="var-list-container">
        <div class="var-list-title">可用字段 (点击插入)</div>
        <div class="var-list">
          <div 
            v-for="f in enabledFields" 
            :key="f.key" 
            class="var-item" 
            @click="insertVar(f.key)"
            :title="f.description"
          >
            <span class="var-name">{{ f.name }}</span>
            <span class="var-key">{{ f.key }}</span>
          </div>
          <el-empty v-if="enabledFields.length === 0" description="暂无可用字段" :image-size="60" />
        </div>
      </div>
    </div>
    
    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="submit" :loading="saving">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { saveField, simulateExpression } from '../api/field'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  fieldData: Object,
  allFields: Array
})

const emit = defineEmits(['update:visible', 'saved'])

const isEdit = computed(() => !!props.fieldData)
const formRef = ref(null)
const form = ref({
  key: '',
  name: '',
  type: 'NUMERIC',
  status: 'ENABLED',
  description: '',
  expression: ''
})
const saving = ref(false)

const rules = {
  key: [{ required: true, message: '业务主键不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '类型不能为空', trigger: 'change' }]
}

// Variables & Simulation
const variables = ref([])
const env = ref({})
const simResult = ref(null)
const simError = ref(false)
const simulating = ref(false)

const enabledFields = computed(() => {
  return props.allFields.filter(f => f.status === 'ENABLED' && f.key !== form.value.key)
})

watch(() => props.visible, (val) => {
  if (val) {
    if (props.fieldData) {
      form.value = { ...props.fieldData }
    } else {
      form.value = {
        key: '',
        name: '',
        type: 'NUMERIC',
        status: 'ENABLED',
        description: '',
        expression: ''
      }
    }
    variables.value = []
    env.value = {}
    simResult.value = null
    parseVariables()
  }
})

const parseVariables = () => {
  if (!form.value.expression) {
    variables.value = []
    return
  }
  // Simple regex to extract possible variables (words that are field keys)
  // Since we don't have aviator frontend parser, we just match words
  const words = form.value.expression.match(/[a-zA-Z_]\w*/g) || []
  const uniqueVars = [...new Set(words)]
  const actualVars = uniqueVars.filter(v => props.allFields.some(f => f.key === v))
  
  variables.value = actualVars
  // Initialize env
  actualVars.forEach(v => {
    if (env.value[v] === undefined) {
      env.value[v] = '1' // default test value
    }
  })
}

const insertVar = (key) => {
  form.value.expression += (form.value.expression ? ' ' : '') + key
  parseVariables()
}

const runSimulation = async () => {
  if (!form.value.expression) return
  simulating.value = true
  simResult.value = null
  simError.value = false
  
  try {
    const formattedEnv = {}
    // convert env values to numbers if applicable
    for (const k in env.value) {
      const val = env.value[k]
      formattedEnv[k] = isNaN(Number(val)) ? val : Number(val)
    }
    
    const res = await simulateExpression({
      expression: form.value.expression,
      env: formattedEnv
    })
    simResult.value = JSON.stringify(res)
  } catch (e) {
    simError.value = true
    simResult.value = e.message
  } finally {
    simulating.value = false
  }
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      saving.value = true
      try {
        await saveField(form.value)
        ElMessage.success('保存成功')
        emit('update:visible', false)
        emit('saved')
      } catch (e) {
        ElMessage.error(e.message) // this will show cycle error if any
      } finally {
        saving.value = false
      }
    }
  })
}
</script>

<style scoped>
.dialog-layout {
  display: flex;
  gap: 20px;
}
.form-container {
  flex: 1;
}
.var-list-container {
  width: 200px;
  border-left: 1px solid #dcdfe6;
  padding-left: 20px;
}
.var-list-title {
  font-weight: bold;
  margin-bottom: 12px;
  color: #606266;
}
.var-list {
  max-height: 400px;
  overflow-y: auto;
}
.var-item {
  padding: 8px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}
.var-item:hover {
  background-color: #ecf5ff;
  border-color: #b3d8ff;
}
.var-name {
  font-size: 13px;
  font-weight: bold;
  color: #303133;
}
.var-key {
  font-size: 12px;
  color: #909399;
}
.w-full {
  width: 100%;
}
.simulation-area {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.sim-title {
  font-weight: bold;
  margin-bottom: 10px;
  color: #606266;
}
.sim-vars {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}
.sim-var-item {
  display: flex;
  align-items: center;
  font-size: 13px;
}
.sim-action {
  display: flex;
  align-items: center;
  gap: 10px;
}
.sim-result {
  font-size: 13px;
}
</style>
