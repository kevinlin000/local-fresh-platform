import { http, unwrap } from '@/services/api'

export interface GroupBuyParticipant {
  memberId: number
  memberName: string
  joinedAt: string
}

export interface GroupBuyRecord {
  id: number
  groupNo: string
  initiatorId: number
  productId: number | null
  productName: string | null
  productImage: string | null
  quantity: number | null
  status: number
  currentCount: number
  requiredCount: number
  expireAt: string
  shareUrl: string | null
  participants: GroupBuyParticipant[]
}

export interface InitiateGroupBuyPayload {
  productId: number
  quantity: number
  addressId: number
  requiredCount: number
}

export interface JoinGroupBuyPayload {
  groupNo: string
  productId: number
  quantity: number
  addressId: number
}

export function initiateGroupBuy(payload: InitiateGroupBuyPayload) {
  return unwrap<GroupBuyRecord>(http.post('/user/groupBuy/initiate', payload))
}

export function joinGroupBuy(payload: JoinGroupBuyPayload) {
  return unwrap<GroupBuyRecord>(http.post('/user/groupBuy/join', payload))
}

export function fetchGroupBuy(groupNo: string) {
  return unwrap<GroupBuyRecord>(http.get(`/user/groupBuy/${groupNo}`))
}

export function fetchMyGroupBuys() {
  return unwrap<GroupBuyRecord[]>(http.get('/user/groupBuy/my'))
}
