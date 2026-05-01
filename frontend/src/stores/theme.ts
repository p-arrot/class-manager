import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { darkTheme, type GlobalTheme, type GlobalThemeOverrides } from 'naive-ui'

// Shared overrides applied to both light and dark
const sharedOverrides: GlobalThemeOverrides = {
  common: {
    borderRadius: '6px',
    borderRadiusSmall: '4px',
    fontFamily: "'Geist', -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif",
    fontWeightStrong: '600',
  },
  Layout: {
    color: '#fafaf9',
    siderColor: '#f5f4f1',
    headerColor: '#fafaf9',
  },
  Menu: {
    itemTextColor: '#4a4a44',
    itemTextColorHover: '#1a1a18',
    itemTextColorActive: '#1a1a18',
    itemColorActive: '#e8e6e1',
    itemColorActiveHover: '#e0ded9',
    itemIconColor: '#8a8a84',
    itemIconColorActive: '#1a1a18',
    itemIconColorHover: '#1a1a18',
    itemHeight: '36px',
    borderRadius: '8px',
  },
  Button: {
    fontWeightText: '500',
    borderRadiusSmall: '6px',
    borderRadiusMedium: '8px',
  },
  Input: {
    borderRadius: '8px',
    border: '1px solid #e8e6e1',
    borderHover: '1px solid #c4c2bc',
    borderFocus: '1px solid #1a1a18',
    boxShadowFocus: '0 0 0 2px rgba(26,26,24,0.06)',
  },
  Card: {
    borderRadius: '12px',
    borderColor: '#eae8e4',
    boxShadow: '0 1px 2px rgba(0,0,0,0.02)',
    titleFontWeight: '600',
  },
  Tag: {
    borderRadius: '6px',
  },
  Select: {
    peers: {
      InternalSelection: {
        borderRadius: '8px',
        border: '1px solid #e8e6e1',
      },
    },
  },
}

const lightOverrides: GlobalThemeOverrides = {
  ...sharedOverrides,
  Layout: {
    color: '#fafaf9',
    siderColor: '#f5f4f1',
    headerColor: '#fafaf9',
    siderBorderColor: '#eae8e4',
    headerBorderColor: '#eae8e4',
  },
}

const darkOverrides: GlobalThemeOverrides = {
  common: {
    ...sharedOverrides.common,
  },
  Layout: {
    color: '#141412',
    siderColor: '#1a1a18',
    headerColor: '#141412',
    siderBorderColor: '#272725',
    headerBorderColor: '#272725',
  },
  Menu: {
    itemTextColor: '#8a8a84',
    itemTextColorHover: '#e8e6e1',
    itemTextColorActive: '#e8e6e1',
    itemColorActive: '#272725',
    itemColorActiveHover: '#2e2e2c',
    itemIconColor: '#5a5a54',
    itemIconColorActive: '#e8e6e1',
    itemIconColorHover: '#e8e6e1',
  },
  Input: {
    border: '1px solid #2e2e2c',
    borderHover: '1px solid #4a4a44',
    borderFocus: '1px solid #e8e6e1',
    boxShadowFocus: '0 0 0 2px rgba(232,230,225,0.06)',
  },
  Card: {
    borderColor: '#272725',
    boxShadow: 'none',
  },
  Select: {
    peers: {
      InternalSelection: {
        border: '1px solid #2e2e2c',
      },
    },
  },
}

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(localStorage.getItem('theme') === 'dark')

  const naiveTheme = computed<GlobalTheme | null>(() =>
    isDark.value ? darkTheme : null
  )

  const themeOverrides = computed<GlobalThemeOverrides>(() =>
    isDark.value ? darkOverrides : lightOverrides
  )

  function toggleTheme() {
    isDark.value = !isDark.value
    localStorage.setItem('theme', isDark.value ? 'dark' : 'light')
  }

  return { isDark, naiveTheme, themeOverrides, toggleTheme }
})
