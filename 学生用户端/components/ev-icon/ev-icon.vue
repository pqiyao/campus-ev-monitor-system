<template>
  <view class="ev-icon" :style="boxStyle">
    <svg
      v-if="name === 'user'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M20 21C20 17.9 16.9 15.5 12 15.5C7.1 15.5 4 17.9 4 21" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
      <circle cx="12" cy="8" r="4" :stroke-width="strokeWidth" />
    </svg>

    <svg
      v-else-if="name === 'lock'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="4.5" y="10.5" width="15" height="10" rx="3" :stroke-width="strokeWidth" />
      <path d="M8 10.5V8.5C8 6 9.8 4 12 4C14.2 4 16 6 16 8.5V10.5" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M12 14V17" :stroke-width="strokeWidth" stroke-linecap="round" />
    </svg>

    <svg
      v-else-if="name === 'mail'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="3.5" y="5.5" width="17" height="13" rx="3" :stroke-width="strokeWidth" />
      <path d="M5 8L12 13L19 8" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'phone'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="7" y="3.5" width="10" height="17" rx="3" :stroke-width="strokeWidth" />
      <path d="M10 6.5H14" :stroke-width="strokeWidth" stroke-linecap="round" />
      <circle cx="12" cy="17.5" r="0.9" fill="currentColor" stroke="none" />
    </svg>

    <svg
      v-else-if="name === 'school'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M3 9L12 4L21 9" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M5 10V19.5H19V10" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <path d="M9 19.5V14H15V19.5" :stroke-width="strokeWidth" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'class'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="3.5" y="5" width="17" height="12" rx="2.5" :stroke-width="strokeWidth" />
      <path d="M8 19.5H16" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M7.5 9H16.5" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M7.5 12.5H13.5" :stroke-width="strokeWidth" stroke-linecap="round" />
    </svg>

    <svg
      v-else-if="name === 'department'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M4 20H20" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M6 20V8L12 5L18 8V20" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <path d="M9.5 11V13.5" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M14.5 11V13.5" :stroke-width="strokeWidth" stroke-linecap="round" />
    </svg>

    <svg
      v-else-if="name === 'bike'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="6.5" cy="17" r="3.5" :stroke-width="strokeWidth" />
      <circle cx="17.5" cy="17" r="3.5" :stroke-width="strokeWidth" />
      <path d="M10 7H13L16 12H11L8.5 8.5" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M9.8 12H16.5" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M7.2 12L9.8 12L6.8 17" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M13 7L12 5.5H10.5" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'frame'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="4" y="5" width="16" height="14" rx="3" :stroke-width="strokeWidth" />
      <path d="M8 10H16" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M8 14H13" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M16.5 14H16.51" :stroke-width="strokeWidth" stroke-linecap="round" />
    </svg>

    <svg
      v-else-if="name === 'spark'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M12 3L13.5 8.5L19 10L13.5 11.5L12 17L10.5 11.5L5 10L10.5 8.5L12 3Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <path d="M18.5 3.5L19 5L20.5 5.5L19 6L18.5 7.5L18 6L16.5 5.5L18 5L18.5 3.5Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'palette'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M12 4C7 4 3.5 7.3 3.5 11.9C3.5 16.1 6.6 19 10.2 19H11.2C12.3 19 13.1 18.1 13.1 17.1C13.1 16.3 13.8 15.7 14.6 15.7H15.8C19.2 15.7 21 13.4 21 10.9C21 6.8 17.2 4 12 4Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <circle cx="8.1" cy="10.1" r="0.9" fill="currentColor" stroke="none" />
      <circle cx="11.2" cy="8.1" r="0.9" fill="currentColor" stroke="none" />
      <circle cx="14.7" cy="8.4" r="0.9" fill="currentColor" stroke="none" />
      <circle cx="16.8" cy="11.1" r="0.9" fill="currentColor" stroke="none" />
    </svg>

    <svg
      v-else-if="name === 'battery'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="3.5" y="7" width="16" height="10" rx="2.5" :stroke-width="strokeWidth" />
      <path d="M20 10V14" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M8.5 9.5L6.8 13H10.2L8.8 16" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M13 9.5H16" :stroke-width="strokeWidth" stroke-linecap="round" />
    </svg>

    <svg
      v-else-if="name === 'location'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M12 20C15.6 15.7 18 13 18 9.8C18 6.6 15.3 4 12 4C8.7 4 6 6.6 6 9.8C6 13 8.4 15.7 12 20Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <circle cx="12" cy="9.5" r="2.2" :stroke-width="strokeWidth" />
    </svg>

    <svg
      v-else-if="name === 'camera'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M4 8.5H7L8.5 6.5H15.5L17 8.5H20C20.8 8.5 21.5 9.2 21.5 10V17C21.5 17.8 20.8 18.5 20 18.5H4C3.2 18.5 2.5 17.8 2.5 17V10C2.5 9.2 3.2 8.5 4 8.5Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <circle cx="12" cy="13.5" r="3.2" :stroke-width="strokeWidth" />
    </svg>

    <svg
      v-else-if="name === 'check'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="12" cy="12" r="8" :stroke-width="strokeWidth" />
      <path d="M8.5 12.3L10.8 14.6L15.5 9.8" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'arrow-right'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M5 12H19" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M13.5 6.5L19 12L13.5 17.5" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'shield'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M12 3.8L19 6.6V11.6C19 16.2 16.2 19.5 12 20.8C7.8 19.5 5 16.2 5 11.6V6.6L12 3.8Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <path d="M9 12.2L11.1 14.3L15.3 10.1" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'warning'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M12 4L20 18H4L12 4Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <path d="M12 9V13" :stroke-width="strokeWidth" stroke-linecap="round" />
      <circle cx="12" cy="16.3" r="0.9" fill="currentColor" stroke="none" />
    </svg>

    <svg
      v-else-if="name === 'card'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <rect x="3.5" y="6" width="17" height="12" rx="3" :stroke-width="strokeWidth" />
      <path d="M3.5 10H20.5" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M7 14.5H10" :stroke-width="strokeWidth" stroke-linecap="round" />
    </svg>

    <svg
      v-else-if="name === 'bell'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M6.5 16H17.5C16.5 15 16 13.8 16 12.4V10.8C16 8.3 14.2 6.2 12 6.2C9.8 6.2 8 8.3 8 10.8V12.4C8 13.8 7.5 15 6.5 16Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <path d="M10 18C10.4 19.1 11.1 19.7 12 19.7C12.9 19.7 13.6 19.1 14 18" :stroke-width="strokeWidth" stroke-linecap="round" />
    </svg>

    <svg
      v-else-if="name === 'appeal'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M5 6.5H19C20.1 6.5 21 7.4 21 8.5V14.5C21 15.6 20.1 16.5 19 16.5H11L7 19V16.5H5C3.9 16.5 3 15.6 3 14.5V8.5C3 7.4 3.9 6.5 5 6.5Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <path d="M8 10.5H16" :stroke-width="strokeWidth" stroke-linecap="round" />
      <path d="M8 13.5H13.5" :stroke-width="strokeWidth" stroke-linecap="round" />
    </svg>

    <svg
      v-else-if="name === 'score'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M12 4.5L13.9 8.4L18.2 9L15.1 12L15.8 16.3L12 14.3L8.2 16.3L8.9 12L5.8 9L10.1 8.4L12 4.5Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <path d="M8.5 17.2V20L12 18.3L15.5 20V17.2" :stroke-width="strokeWidth" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'settings'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M12 8.7C10.2 8.7 8.7 10.2 8.7 12C8.7 13.8 10.2 15.3 12 15.3C13.8 15.3 15.3 13.8 15.3 12C15.3 10.2 13.8 8.7 12 8.7Z" :stroke-width="strokeWidth" />
      <path d="M19 13.2V10.8L16.9 10L16.3 8.4L17.2 6.3L15.7 4.8L13.6 5.7L12 5.1L11.2 3H8.8L8 5.1L6.4 5.7L4.3 4.8L2.8 6.3L3.7 8.4L3.1 10L1 10.8V13.2L3.1 14L3.7 15.6L2.8 17.7L4.3 19.2L6.4 18.3L8 18.9L8.8 21H11.2L12 18.9L13.6 18.3L15.7 19.2L17.2 17.7L16.3 15.6L16.9 14L19 13.2Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'logout'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M10 5H6C4.9 5 4 5.9 4 7V17C4 18.1 4.9 19 6 19H10" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M13 8L17 12L13 16" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M9 12H17" :stroke-width="strokeWidth" stroke-linecap="round" />
    </svg>

    <svg
      v-else-if="name === 'folder'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M3.5 8.5C3.5 7.4 4.4 6.5 5.5 6.5H10L11.8 8.2H18.5C19.6 8.2 20.5 9.1 20.5 10.2V16.5C20.5 17.6 19.6 18.5 18.5 18.5H5.5C4.4 18.5 3.5 17.6 3.5 16.5V8.5Z" :stroke-width="strokeWidth" stroke-linejoin="round" />
    </svg>

    <svg
      v-else-if="name === 'radar'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="12" cy="12" r="7.5" :stroke-width="strokeWidth" />
      <circle cx="12" cy="12" r="3.5" :stroke-width="strokeWidth" />
      <path d="M12 12L17 7" :stroke-width="strokeWidth" stroke-linecap="round" />
      <circle cx="17" cy="7" r="1.1" fill="currentColor" stroke="none" />
    </svg>

    <svg
      v-else-if="name === 'home'"
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M4 10L12 4L20 10" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
      <path d="M6 9.8V19H18V9.8" :stroke-width="strokeWidth" stroke-linejoin="round" />
      <path d="M10 19V13H14V19" :stroke-width="strokeWidth" stroke-linejoin="round" />
    </svg>

    <svg
      v-else
      class="svg"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
    >
      <circle cx="12" cy="12" r="8" :stroke-width="strokeWidth" />
      <path d="M12 8V12L15 14" :stroke-width="strokeWidth" stroke-linecap="round" stroke-linejoin="round" />
    </svg>
  </view>
</template>

<script>
export default {
  props: {
    name: {
      type: String,
      default: 'home'
    },
    size: {
      type: [Number, String],
      default: 36
    },
    color: {
      type: String,
      default: 'currentColor'
    },
    strokeWidth: {
      type: [Number, String],
      default: 1.8
    }
  },
  computed: {
    boxStyle() {
      const size = typeof this.size === 'number' ? `${this.size}rpx` : this.size
      return {
        width: size,
        height: size,
        color: this.color
      }
    }
  }
}
</script>

<style scoped>
.ev-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
  flex-shrink: 0;
}

.svg {
  width: 100%;
  height: 100%;
  stroke: currentColor;
}
</style>
