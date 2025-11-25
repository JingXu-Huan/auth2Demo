import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/chat',
    children: [
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('../views/Chat.vue'),
        meta: { title: '消息' }
      },
      {
        path: 'contacts',
        name: 'Contacts',
        component: () => import('../views/Contacts.vue'),
        meta: { title: '通讯录' }
      },
      {
        path: 'documents',
        name: 'Documents',
        component: () => import('../views/Documents.vue'),
        meta: { title: '云文档' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('../views/Settings.vue'),
        meta: { title: '设置' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.meta.guest) {
    // 访客页面（登录/注册），已登录则跳转首页
    if (token) {
      next('/')
    } else {
      next()
    }
  } else {
    // 需要登录的页面
    if (token) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
