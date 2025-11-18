import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../layout/Layout.vue'

const routes = [
  {
    path: '/',
    redirect: '/im'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/test',
    name: 'Test',
    component: () => import('../views/Test.vue')
  },
  {
    path: '/oauth/callback',
    name: 'OAuthCallback',
    component: () => import('../views/OAuthCallback.vue')
  },
  {
    path: '/im',
    component: Layout,
    redirect: '/im/messages',
    children: [
      {
        path: 'messages',
        name: 'Messages',
        component: () => import('../views/Messages.vue'),
        meta: { title: '消息', icon: 'ChatDotRound' }
      },
      {
        path: 'contacts',
        name: 'Contacts',
        component: () => import('../views/Contacts.vue'),
        meta: { title: '通讯录', icon: 'User' }
      },
      {
        path: 'groups',
        name: 'Groups',
        component: () => import('../views/Groups.vue'),
        meta: { title: '群聊', icon: 'UserFilled' }
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
  
  // 不需要登录的页面
  const publicPages = ['/login', '/test', '/oauth/callback']
  
  if (publicPages.includes(to.path)) {
    // 如果是登录页且已登录，跳转到主页
    if (to.path === '/login' && token) {
      next('/im')
    } else {
      next()
    }
  } else {
    // 其他页面需要登录
    if (token) {
      next()
    } else {
      next('/login')
    }
  }
})

export default router
