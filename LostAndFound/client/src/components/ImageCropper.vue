<template>
  <div class="cropper-overlay">
    <div class="cropper-modal">
      <h3>裁切头像</h3>
      <div class="cropper-area" ref="areaRef" @mousedown="startDrag" @touchstart.prevent="startDrag"
           @wheel.prevent="handleWheel">
        <img :src="imgUrl" ref="imgRef" :style="imgStyle" class="source-img" alt="Source" draggable="false" @dragstart.prevent />
        <div class="viewport-mask">
          <div class="grid-lines"></div>
        </div>
      </div>
      
      <div class="controls">
        <div class="slider-group">
          <label>缩放</label>
          <input type="range" min="0.02" max="50" step="0.01" v-model.number="scale" />
        </div>
        <div class="btns">
          <button @click="$emit('cancel')">取消</button>
          <button @click="confirm" class="primary">确定</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps<{ file: File }>()
const emit = defineEmits(['confirm', 'cancel'])

const imgUrl = ref('')
const scale = ref(1)
const translateX = ref(0)
const translateY = ref(0)
const isDragging = ref(false)
const startX = ref(0)
const startY = ref(0)
const startTx = ref(0)
const startTy = ref(0)

const imgRef = ref<HTMLImageElement>()
const areaRef = ref<HTMLDivElement>()

const VIEWPORT_SIZE = 200 // 200x200 square crop

const imgStyle = computed(() => ({
  transform: `translate(-50%, -50%) translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value})`,
  cursor: isDragging.value ? 'grabbing' : 'grab'
}))

// Load image
watch(() => props.file, (f) => {
  if (f) {
    const reader = new FileReader()
    reader.onload = (e) => {
      imgUrl.value = e.target?.result as string
      // Reset
      scale.value = 1
      translateX.value = 0
      translateY.value = 0
    }
    reader.readAsDataURL(f)
  }
}, { immediate: true })

const startDrag = (e: MouseEvent | TouchEvent) => {
  isDragging.value = true
  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY
  
  startX.value = clientX
  startY.value = clientY
  startTx.value = translateX.value
  startTy.value = translateY.value
  
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
  document.addEventListener('touchmove', onDrag)
  document.addEventListener('touchend', stopDrag)
}

const onDrag = (e: MouseEvent | TouchEvent) => {
  if (!isDragging.value) return
  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY
  
  translateX.value = startTx.value + (clientX - startX.value)
  translateY.value = startTy.value + (clientY - startY.value)
}

const stopDrag = () => {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDrag)
  document.removeEventListener('touchend', stopDrag)
}

const handleWheel = (e: WheelEvent) => {
  const delta = e.deltaY > 0 ? 0.9 : 1.1
  const newScale = scale.value * delta
  scale.value = Math.max(0.02, Math.min(50, newScale))
}

const confirm = () => {
  if (!imgRef.value || !areaRef.value) return
  
  // Use getBoundingClientRect for WYSIWYG cropping
  // 1. Get the geometry of the Crop Area (Container)
  const areaRect = areaRef.value.getBoundingClientRect()
  
  // 2. Calculate the "Visual Viewport" rect in Client Coordinates
  // The viewport is a 200x200 box centered in the 300x300 area
  const VIEWPORT_W = 200
  const VIEWPORT_H = 200
  
  const cx = areaRect.left + areaRect.width / 2
  const cy = areaRect.top + areaRect.height / 2
  
  const vLeft = cx - VIEWPORT_W / 2
  const vTop = cy - VIEWPORT_H / 2
  
  // 3. Get the geometry of the Image
  const imgRect = imgRef.value.getBoundingClientRect()
  
  // 4. Calculate ratio between "Natural Image" and "Displayed Image"
  const scaleX = imgRef.value.naturalWidth / imgRect.width
  const scaleY = imgRef.value.naturalHeight / imgRect.height
  
  // 5. Calculate the source coordinates (What part of the image is under the viewport?)
  // (vLeft - imgRect.left) is the distance in pixels from Image Left to Viewport Left
  const sx = (vLeft - imgRect.left) * scaleX
  const sy = (vTop - imgRect.top) * scaleY
  const sWidth = VIEWPORT_W * scaleX
  const sHeight = VIEWPORT_H * scaleY
  
  // 6. Draw to Canvas
  // We export at 400x400 for better quality (Retina ready)
  const EXPORT_SIZE = 400
  const canvas = document.createElement('canvas')
  canvas.width = EXPORT_SIZE
  canvas.height = EXPORT_SIZE
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  
  // Fill white background (transparency safety)
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, EXPORT_SIZE, EXPORT_SIZE)
  
  ctx.drawImage(
    imgRef.value,
    sx, sy, sWidth, sHeight,   // Source crop
    0, 0, EXPORT_SIZE, EXPORT_SIZE // Destination fill
  )
  
  canvas.toBlob((blob) => {
    emit('confirm', blob)
  }, 'image/jpeg', 0.95)
}
</script>

<style scoped>
.cropper-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.6);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.cropper-modal {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 340px;
}
.cropper-area {
  width: 300px;
  height: 300px;
  background: #333;
  margin: 0 auto 20px;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  cursor: grab;
}
.cropper-area:active {
  cursor: grabbing;
}
.source-img {
  max-width: none; /* Allow scaling */
  display: block;
  /* Initial position is centered by flex, but we use transform for movement */
  position: absolute; /* To allow free movement */
  /* Center manually to make math easier? No, flex center puts it in center. */
  /* If we use absolute, we need to center it first. */
  top: 50%; left: 50%;
  transform-origin: center;
  /* We will subtract 50% in transform if we use top:50% left:50%, or just rely on flex centering? */
  /* Using flex centering is tricky with drag. Let's use absolute centering. */
  margin-top: -50%; /* This depends on height... tricky. */
  /* Let's stick to: img is naturally centered if we don't set top/left? */
  /* Simpler: CSS transform translate moves it from its original position. */
}
/* Wait, if I use flex center, the image is centered. 
   Then transform translate moves it relative to that center.
   That matches my math (renderedCenterX = containerW/2 + Tx).
   Correct.
*/

.viewport-mask {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 200px;
  height: 200px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5);
  pointer-events: none; /* Let clicks pass through to image */
}

.grid-lines {
  width: 100%;
  height: 100%;
  position: relative;
}
.grid-lines::before,
.grid-lines::after {
  content: '';
  position: absolute;
  top: 0; bottom: 0;
  border-left: 1px dashed rgba(255, 255, 255, 0.5);
  border-right: 1px dashed rgba(255, 255, 255, 0.5);
}
.grid-lines::before {
  left: 33.33%;
  width: 33.33%;
  border-right: none;
}
.grid-lines::after {
  right: 33.33%;
  width: 33.33%;
  border-left: none;
}
/* Horizontal lines */
.grid-lines {
  background: 
    linear-gradient(to bottom, transparent 33.33%, rgba(255,255,255,0.5) 33.33%, rgba(255,255,255,0.5) calc(33.33% + 1px), transparent calc(33.33% + 1px)),
    linear-gradient(to bottom, transparent 66.66%, rgba(255,255,255,0.5) 66.66%, rgba(255,255,255,0.5) calc(66.66% + 1px), transparent calc(66.66% + 1px));
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.slider-group {
  display: flex;
  align-items: center;
  gap: 10px;
}
.slider-group input { flex: 1; }
.btns {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
button {
  padding: 6px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
}
button.primary {
  background: #1890ff;
  color: white;
  border-color: #1890ff;
}
</style>
