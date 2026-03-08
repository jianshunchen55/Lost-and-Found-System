<template>
  <div class="map-container" ref="container">
    <img :src="mapSrc" class="map-img" @click="pick" ref="mapImg" />
    
    <!-- Known Points -->
    <div 
      v-for="p in points" 
      :key="p.id"
      class="map-point"
      :class="{active: selectedId === p.id}"
      :style="{left: p.longitude + '%', top: p.latitude + '%'}"
      @click.stop="selectPoint(p)"
    >
      <div class="point-label">{{ p.name }}</div>
    </div>

    <!-- Temporary Click Point (if not matching any known point) -->
    <div 
      v-if="!selectedId && tempPoint" 
      class="map-point temp" 
      :style="{left: tempPoint.x + '%', top: tempPoint.y + '%'}"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{ 
  mapSrc: string,
  points?: any[],
  selectedId?: number | null,
  initialPoint?: {x: number, y: number} | null,
  readonly?: boolean
}>()

const emit = defineEmits(['select'])
const tempPoint = ref<{x:number, y:number} | null>(null)
const mapImg = ref<HTMLImageElement>()

watch(() => props.initialPoint, (val) => {
  if (val) {
    tempPoint.value = val
  }
}, { immediate: true })

function pick(e: MouseEvent) {
  if (props.readonly) return
  if (!mapImg.value) return
  
  const rect = mapImg.value.getBoundingClientRect()
  // Calculate percentage relative to image dimensions
  const x = ((e.clientX - rect.left) / rect.width) * 100
  const y = ((e.clientY - rect.top) / rect.height) * 100
  
  // Clamp to 0-100
  const clampedX = Math.max(0, Math.min(100, x))
  const clampedY = Math.max(0, Math.min(100, y))
  
  tempPoint.value = { x: clampedX, y: clampedY }
  // Emit generic point (no ID)
  emit('select', { id: null, x: clampedX, y: clampedY })
}

function selectPoint(p: any) {
  emit('select', p)
}
</script>

<style scoped>
.map-container {
  position: relative;
  display: inline-block;
  overflow: hidden;
  border: 1px solid #eee;
  border-radius: 4px;
}
.map-img {
  display: block;
  max-width: 100%;
  cursor: crosshair;
}
.map-point {
  position: absolute;
  width: 12px;
  height: 12px;
  background: #1890ff;
  border: 2px solid white;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  cursor: pointer;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(0,0,0,0.2);
}
.map-point.active {
  background: #ff4d4f;
  transform: translate(-50%, -50%) scale(1.2);
  z-index: 11;
}
.map-point.temp {
  background: #52c41a;
}
.point-label {
  position: absolute;
  top: -24px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  white-space: nowrap;
  display: none;
  pointer-events: none;
}
.map-point:hover .point-label {
  display: block;
}
</style>
