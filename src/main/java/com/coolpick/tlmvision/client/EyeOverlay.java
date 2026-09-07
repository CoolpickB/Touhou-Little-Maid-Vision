package com.coolpick.tlmvision.client;
import com.coolpick.tlmvision.TlmVisionHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;
/** Soft shadow and drifting third-eye cords, drawn beneath the HUD. */
@EventBusSubscriber(modid=TlmVisionHelper.MOD_ID,value=Dist.CLIENT)
public final class EyeOverlay {
 private static long started,finished;
 private static boolean reply;
 private static Component name;
 private static long now(){return System.nanoTime()/1_000_000;}
 static void begin(Component n){started=now();finished=0;name=n;reply=false;}
 static void waitingForReply(){reply=true;}
 static void finish(){finished=now();}
 static void clear(){started=0;finished=0;reply=false;}
 private static boolean hidden(Minecraft mc){return started==0||mc.screen!=null||mc.options.hideGui||MaidCamera.isCapturing();}
 private static float age(){return (now()-started)/1000f;}
 private static float closing(){return finished==0?-1:(now()-finished)/900f;}
 /** The frame is a world effect, so it renders under the hotbar, chat and the rest of the HUD. */
 @SubscribeEvent public static void renderFrame(RenderGuiEvent.Pre event){
  Minecraft mc=Minecraft.getInstance();
  if(hidden(mc))return;
  frame(event.getGuiGraphics(),age(),closing());
 }
 @SubscribeEvent public static void renderEyes(RenderGuiEvent.Post event){
  Minecraft mc=Minecraft.getInstance();
  boolean overlay=!hidden(mc);
  if(overlay)eyes(event.getGuiGraphics(),mc,age(),closing());
  borrowLine(event.getGuiGraphics(),mc,overlay&&closing()<0);
 }
 /** The borrowed camera has no capture animation, so its own line reuses the status widget. */
 private static void borrowLine(GuiGraphics g,Minecraft mc,boolean stacked){
  if(!MaidCamera.isBorrowing()||mc.screen!=null||mc.options.hideGui||MaidCamera.isCapturing())return;
  line(g,mc,MaidCamera.borrowLabel(),g.guiHeight()-(stacked?56:39),0xFFD8C5C5);
 }
 /** The frame belongs to the capture, not to the wait: it is gone by ENDED, whatever the request is doing. */
 private static final float HOLD=.85f,ENDED=1.5f;
 private static float envelope(float age,float closing){
  float out=age<HOLD?1:1-(age-HOLD)/(ENDED-HOLD);
  if(closing>=0)out=Math.min(out,1-Math.min(1,closing));
  return Math.max(0,out)*smooth(Math.min(1,age/.18f));
 }
 static void frame(GuiGraphics g,float age,float closing){
  if(age>=ENDED)return;
  int w=g.guiWidth(),h=g.guiHeight();float fade=envelope(age,closing);
  if(fade<=0)return;
  g.flush();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(GameRenderer::getPositionColorShader);
  BufferBuilder buffer=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
  Matrix4f matrix=g.pose().last().pose();
  // A transparent center with a broad, smooth falloff into near-black corners.
  float[] radii={.38f,.60f,.78f,.94f,1.12f,1.45f};
  int[] alpha={0,8,35,88,158,205};
  for(int band=0;band<radii.length-1;band++)for(int i=0;i<128;i++){
   double a=i*Math.PI*2/128,b=(i+1)*Math.PI*2/128;
   vignetteVertex(buffer,matrix,w,h,a,radii[band],alpha[band],fade);
   vignetteVertex(buffer,matrix,w,h,b,radii[band],alpha[band],fade);
   vignetteVertex(buffer,matrix,w,h,b,radii[band+1],alpha[band+1],fade);
   vignetteVertex(buffer,matrix,w,h,a,radii[band+1],alpha[band+1],fade);
  }
  // Broken, overlapping arcs feel like loose cords, rather than an enclosing rim.
  for(int strand=0;strand<8;strand++){
   cord(buffer,matrix,w,h,strand,age,fade,3.6f,0x100708,.32f);
   cord(buffer,matrix,w,h,strand,age,fade,1.65f,0x702222,.85f);
   cord(buffer,matrix,w,h,strand,age,fade,.58f,0xa73333,.65f);
  }
  BufferUploader.drawWithShader(buffer.buildOrThrow());RenderSystem.disableBlend();
 }
 private static float smooth(float t){return t*t*(3-2*t);}
 private static void vignetteVertex(BufferBuilder buf,Matrix4f mat,int w,int h,double angle,float radius,int alpha,float fade){
  point(buf,mat,w*.5+Math.cos(angle)*w*.5*radius,h*.5+Math.sin(angle)*h*.5*radius,0x080609,alpha*fade);
 }
 /** Slow traveling bends and a moving highlight give the cords a crawling motion. */
 private static void cord(BufferBuilder buf,Matrix4f mat,int w,int h,int strand,float age,float fade,float width,int color,float opacity){
  double scale=Math.min(w,h)/360.0;
  for(int i=0;i<80;i++){
   double t=i/80.0,u=(i+1)/80.0;
   double x=cordX(w,strand,t,age),y=cordY(h,strand,t,age);
   double nx=cordX(w,strand,u,age),ny=cordY(h,strand,u,age);
   double dx=nx-x,dy=ny-y,len=Math.max(.001,Math.hypot(dx,dy));
   double taper=Math.pow(Math.sin(Math.PI*t),.65),nextTaper=Math.pow(Math.sin(Math.PI*u),.65);
   double ox=-dy/len*width*scale,oy=dx/len*width*scale;
   float light=(float)(.74+.26*Math.sin(t*12-age*4+strand*1.7));
   float a=255*fade*opacity*light;
   point(buf,mat,x-ox*taper,y-oy*taper,color,a);
   point(buf,mat,nx-ox*nextTaper,ny-oy*nextTaper,color,a);
   point(buf,mat,nx+ox*nextTaper,ny+oy*nextTaper,color,a);
   point(buf,mat,x+ox*taper,y+oy*taper,color,a);
  }
 }
 private static double cordAngle(int strand,double t,float age){
  return strand*Math.PI/4+(t-.5)*(1.0+(strand%3)*.19)+age*(strand%2==0?.09:-.07);
 }
 private static double cordRadius(int strand,double t,float age){
  return .94+.055*Math.sin(t*9-age*1.8+strand*2.3)+.025*Math.sin(t*19+age*1.2+strand)+.035*(strand%2);
 }
 private static double cordX(int w,int strand,double t,float age){
  return w*.5+w*.5*Math.cos(cordAngle(strand,t,age))*cordRadius(strand,t,age);
 }
 private static double cordY(int h,int strand,double t,float age){
  return h*.5+h*.5*Math.sin(cordAngle(strand,t,age))*cordRadius(strand,t,age);
 }
 private static void point(BufferBuilder buf,Matrix4f mat,double x,double y,int color,float alpha){
  buf.addVertex(mat,(float)x,(float)y,0).setColor((color>>16)&255,(color>>8)&255,color&255,(int)alpha);
 }
 static void eyes(GuiGraphics g,Minecraft mc,float age,float closing){
  if(closing>=0)return;
  boolean fresh=age<1.15f;
  Component status=fresh?Component.translatable("tlmvision.eye.captured"):Component.translatable(reply?"tlmvision.eye.reply":"tlmvision.eye.sharing",name);
  line(g,mc,status,g.guiHeight()-39,fresh?0xFFE5CACA:0xFFD8C5C5);
 }
 private static void line(GuiGraphics g,Minecraft mc,Component text,int y,int color){
  int w=g.guiWidth(),tw=mc.font.width(text);
  g.fill(w/2-tw/2-7,y-4,w/2+tw/2+7,y+13,0xD9100B0D);
  g.drawCenteredString(mc.font,text,w/2,y,color);
 }
}
