class Inheritance {
    public static void main(String[] a){
	System.out.println(new MainClass().Start());
    }
}

class MainClass {
	public int Start() {
		int v;
		
		Animal animal;
		Animal bird;
		Animal dog;
		
		animal = new Animal();
		bird = new Bird();
		dog = new Dog();
		
		v = animal.init();
		v = animal.sleep();
		v = animal.eat();
		v = animal.exit();
		
		v = bird.init();
		v = bird.sleep();
		v = bird.eat();
		v = bird.exit();
		
		v = dog.init();
		v = dog.sleep();
		v = dog.eat();
		v = dog.exit();
		
		return 0;
	}
}

class Animal {
	int age;
	
	public int init() {
		age = 0;
		System.out.println(age);
		return 1;
	}
	
	public int sleep() {
		age = age + 1;
		System.out.println(age);
		return 2;
	}
	
	public int eat() {
		age = age + 10;
		System.out.println(age);
		return 3;
	}
	
	public int exit() {
		System.out.println(age);
		return 4;
	}
}

class Bird extends Animal {
	int feathers;
	
	public int init() {
		age = 1000;
		feathers = 0;
		System.out.println(age + feathers);
		return 5;
	}
	
	public int sleep() {
		feathers = feathers + 100;
		System.out.println(feathers);
		return 7;
	}
	
	public int exit() {
		System.out.println(age + feathers);
		return 8;
	}
}

class Dog extends Animal {
	int tails;
	
	public int init() {
		age = 2000;
		tails = 0;
		System.out.println(age + tails);
		return 9;
	}
	
	public int eat() {
		tails = tails + 200;
		System.out.println(tails);
		return 10;
	}
	
	public int exit() {
		System.out.println(age + tails);
		return 12;
	}
}
