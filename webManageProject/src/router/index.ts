import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '首页 - 运动会列表' },
      },
      {
        path: 'meeting/:id',
        name: 'MeetingDetail',
        component: () => import('@/views/MeetingDetailView.vue'),
        meta: { title: '运动会管理' },
      },
      {
        path: 'meeting/:meetingId/schedule/:scheduleId',
        name: 'ScheduleDetail',
        component: () => import('@/views/ScheduleDetailView.vue'),
        meta: { title: '赛程管理' },
      },
      {
        path: 'meeting/:meetingId/schedule/:scheduleId/event/:eventId',
        name: 'EventDetail',
        component: () => import('@/views/EventDetailView.vue'),
        meta: { title: '项目报名' },
      },
      {
        path: 'meeting/:meetingId/team/:teamId',
        name: 'TeamDetail',
        component: () => import('@/views/TeamDetailView.vue'),
        meta: { title: '代表队详情' },
      },
      {
        path: 'meeting/:meetingId/participant/:participantId',
        name: 'ParticipantDetail',
        component: () => import('@/views/ParticipantDetailView.vue'),
        meta: { title: '参赛人员详情' },
      },
      {
        path: 'record',
        name: 'Record',
        component: () => import('@/views/RecordView.vue'),
        meta: { title: '记录管理' },
      },
      {
        path: 'banner',
        name: 'Banner',
        component: () => import('@/views/BannerView.vue'),
        meta: { title: '轮播图管理' },
      },
      {
        path: 'global-notice',
        name: 'GlobalNotice',
        component: () => import('@/views/GlobalNoticeView.vue'),
        meta: { title: '全局通知管理' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || '后台管理系统'
  next()
})

export default router
